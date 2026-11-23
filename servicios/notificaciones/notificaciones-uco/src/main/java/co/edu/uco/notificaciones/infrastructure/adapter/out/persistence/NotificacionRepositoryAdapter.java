package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import co.edu.uco.notificaciones.application.model.FalloReciente;
import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResultadoReintento;
import co.edu.uco.notificaciones.application.model.ResumenCola;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.domain.Notificacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA de la cola. Las escrituras de estado son condicionales a la reserva de la instancia. */
@Repository
public class NotificacionRepositoryAdapter implements NotificacionRepositoryPort {

    private static final TypeReference<List<Destinatario>> TIPO_DESTINATARIOS = new TypeReference<>() { };
    private static final TypeReference<Map<String, String>> TIPO_DATOS = new TypeReference<>() { };
    private static final List<EstadoEnvio> REINTENTABLES = List.of(EstadoEnvio.PENDIENTE, EstadoEnvio.FALLIDA_REINTENTABLE);
    private static final List<EstadoEnvio> TERMINALES = List.of(EstadoEnvio.ENTREGADA, EstadoEnvio.FALLIDA_DEFINITIVA);
    private static final int MAX_CODIGO = 40;
    private static final int MAX_DETALLE = 300;

    private final NotificacionJpaRepository jpa;
    private final JsonMapper json;
    private final TransactionTemplate transacciones;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificacionRepositoryAdapter(final NotificacionJpaRepository jpa, final JsonMapper json, final PlatformTransactionManager manager) {
        this.jpa = Objects.requireNonNull(jpa);
        this.json = Objects.requireNonNull(json);
        this.transacciones = new TransactionTemplate(manager);
    }

    @Override
    public ResultadoRegistro registrarSiNoExiste(final Notificacion nueva) {
        final Optional<NotificacionEntity> existente = jpa.findByAplicacionAndIdempotencyKey(nueva.aplicacion(), nueva.idempotencyKey());
        if (existente.isPresent()) {
            return new ResultadoRegistro(aDominio(existente.get()), false);
        }
        try {
            final NotificacionEntity guardada = transacciones.execute(status -> jpa.saveAndFlush(aEntidad(nueva)));
            return new ResultadoRegistro(aDominio(Objects.requireNonNull(guardada)), true);
        } catch (final DataIntegrityViolationException ex) {
            // Carrera con otra solicitud de la misma clave: gana la primera y esta devuelve la misma notificacion.
            return jpa.findByAplicacionAndIdempotencyKey(nueva.aplicacion(), nueva.idempotencyKey())
                    .map(e -> new ResultadoRegistro(aDominio(e), false))
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notificacion> buscarPorId(final UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    @Override
    @Transactional
    public List<Notificacion> reservar(final int lote, final Duration lease, final String instancia, final Instant ahora) {
        final List<NotificacionEntity> elegibles = jpa.elegibles(REINTENTABLES, EstadoEnvio.RESERVADA, ahora, PageRequest.of(0, lote));
        final Instant vence = ahora.plus(lease);
        final List<Notificacion> reservadas = new ArrayList<>(elegibles.size());
        for (final NotificacionEntity e : elegibles) {
            e.setEstado(EstadoEnvio.RESERVADA);
            e.setReservadoHasta(vence);
            e.setReservadoPor(instancia);
            e.setIntentos(e.getIntentos() + 1);
            reservadas.add(aDominio(e));
        }
        return reservadas;
    }

    @Override
    @Transactional
    public boolean marcarEntregada(final UUID id, final String instancia, final Instant ahora) {
        return jpa.marcarEntregada(id, instancia, ahora, EstadoEnvio.ENTREGADA, EstadoEnvio.RESERVADA) > 0;
    }

    @Override
    @Transactional
    public boolean registrarFallo(final UUID id, final String instancia, final String codigoError, final String detalle,
                                  final boolean reintentable, final int maxIntentos, final Duration backoffBase,
                                  final Duration backoffMax, final Instant ahora) {
        final Optional<NotificacionEntity> encontrada = jpa.findById(id);
        if (encontrada.isEmpty() || encontrada.get().getEstado() != EstadoEnvio.RESERVADA
                || !Objects.equals(instancia, encontrada.get().getReservadoPor())) {
            return false;
        }
        final NotificacionEntity e = encontrada.get();
        if (!reintentable || e.getIntentos() >= maxIntentos) {
            e.setEstado(EstadoEnvio.FALLIDA_DEFINITIVA);
            e.setProximoIntentoEn(null);
        } else {
            e.setEstado(EstadoEnvio.FALLIDA_REINTENTABLE);
            e.setProximoIntentoEn(ahora.plus(calcularBackoff(e.getIntentos(), backoffBase, backoffMax)));
        }
        e.setReservadoHasta(null);
        e.setReservadoPor(null);
        e.setFalloEn(ahora);
        e.setCodigoError(recortar(codigoError == null ? "ERR_DESCONOCIDO" : codigoError, MAX_CODIGO));
        e.setDetalleError(recortar(detalle, MAX_DETALLE));
        return true;
    }

    /** MIN(base por 2 elevado a (intentos - 1), max). */
    public static Duration calcularBackoff(final int intentos, final Duration base, final Duration max) {
        final int exponente = Math.clamp(intentos - 1L, 0, 30);
        final long segundos = base.toSeconds() * (1L << exponente);
        return segundos >= max.toSeconds() || segundos < 0 ? max : Duration.ofSeconds(segundos);
    }

    @Override
    @Transactional
    public int liberar(final List<UUID> ids, final String instancia, final Instant ahora) {
        int liberadas = 0;
        for (final NotificacionEntity e : jpa.findAllById(ids)) {
            if (e.getEstado() == EstadoEnvio.RESERVADA && Objects.equals(instancia, e.getReservadoPor())) {
                e.setEstado(EstadoEnvio.PENDIENTE);
                e.setIntentos(Math.max(0, e.getIntentos() - 1));
                e.setProximoIntentoEn(ahora);
                e.setReservadoHasta(null);
                e.setReservadoPor(null);
                liberadas++;
            }
        }
        return liberadas;
    }

    @Override
    @Transactional
    public ResultadoReintento reencolar(final UUID id, final Instant ahora) {
        final Optional<NotificacionEntity> encontrada = jpa.findById(id);
        if (encontrada.isEmpty()) {
            return ResultadoReintento.NO_EXISTE;
        }
        final NotificacionEntity e = encontrada.get();
        if (e.getEstado() != EstadoEnvio.FALLIDA_DEFINITIVA && e.getEstado() != EstadoEnvio.FALLIDA_REINTENTABLE) {
            return ResultadoReintento.ESTADO_INVALIDO;
        }
        e.setEstado(EstadoEnvio.PENDIENTE);
        e.setIntentos(0);
        e.setProximoIntentoEn(ahora);
        e.setReservadoHasta(null);
        e.setReservadoPor(null);
        e.setCodigoError(null);
        e.setDetalleError(null);
        e.setFalloEn(null);
        return ResultadoReintento.REENCOLADA;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaNotificaciones consultar(final FiltroNotificaciones filtro, final int pagina, final int tamano) {
        final EstadoEnvio estado = filtro.estado() == null ? null : EstadoEnvio.valueOf(filtro.estado());
        final StringBuilder condiciones = new StringBuilder(" where 1 = 1");
        final Map<String, Object> parametros = new LinkedHashMap<>();
        if (estado != null) {
            condiciones.append(" and n.estado = :estado");
            parametros.put("estado", estado);
        }
        if (filtro.tipo() != null) {
            condiciones.append(" and n.tipo = :tipo");
            parametros.put("tipo", filtro.tipo());
        }
        if (filtro.aplicacion() != null) {
            condiciones.append(" and n.aplicacion = :aplicacion");
            parametros.put("aplicacion", filtro.aplicacion());
        }
        final TypedQuery<Long> conteo = entityManager.createQuery("select count(n) from NotificacionEntity n" + condiciones, Long.class);
        parametros.forEach(conteo::setParameter);
        final long total = conteo.getSingleResult();

        final TypedQuery<NotificacionEntity> consulta = entityManager.createQuery(
                "select n from NotificacionEntity n" + condiciones + " order by n.creadoEn desc, n.id desc", NotificacionEntity.class);
        parametros.forEach(consulta::setParameter);
        consulta.setFirstResult((pagina - 1) * tamano);
        consulta.setMaxResults(tamano);
        final List<Notificacion> elementos = consulta.getResultList().stream().map(this::aDominio).toList();
        final int totalPaginas = (int) Math.max(1, (total + tamano - 1) / tamano);
        return new PaginaNotificaciones(elementos, pagina, tamano, total, totalPaginas, resumen(filtro));
    }

    private ResumenCola resumen(final FiltroNotificaciones filtro) {
        final StringBuilder jpql = new StringBuilder("select n.estado, count(n) from NotificacionEntity n where 1 = 1");
        final Map<String, Object> parametros = new LinkedHashMap<>();
        if (filtro.tipo() != null) {
            jpql.append(" and n.tipo = :tipo");
            parametros.put("tipo", filtro.tipo());
        }
        if (filtro.aplicacion() != null) {
            jpql.append(" and n.aplicacion = :aplicacion");
            parametros.put("aplicacion", filtro.aplicacion());
        }
        jpql.append(" group by n.estado");
        final TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        parametros.forEach(query::setParameter);
        final Map<EstadoEnvio, Long> conteos = new EnumMap<>(EstadoEnvio.class);
        for (final Object[] fila : query.getResultList()) {
            conteos.put((EstadoEnvio) fila[0], (Long) fila[1]);
        }
        return new ResumenCola(
                conteos.getOrDefault(EstadoEnvio.PENDIENTE, 0L),
                conteos.getOrDefault(EstadoEnvio.RESERVADA, 0L),
                conteos.getOrDefault(EstadoEnvio.ENTREGADA, 0L),
                conteos.getOrDefault(EstadoEnvio.FALLIDA_REINTENTABLE, 0L),
                conteos.getOrDefault(EstadoEnvio.FALLIDA_DEFINITIVA, 0L));
    }

    @Override
    @Transactional
    public int depurar(final Instant limite, final int lote) {
        final List<UUID> ids = jpa.terminalesAnteriores(TERMINALES, limite, PageRequest.of(0, lote));
        return ids.isEmpty() ? 0 : jpa.eliminarPorIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> ultimaEntrega() {
        return jpa.ultimaEntrega();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FalloReciente> ultimoFallo() {
        return jpa.ultimosFallos(PageRequest.of(0, 1)).stream().findFirst()
                .map(e -> new FalloReciente(e.getFalloEn(), e.getCodigoError()));
    }

    private NotificacionEntity aEntidad(final Notificacion n) {
        final NotificacionEntity e = new NotificacionEntity(n.id());
        e.setAplicacion(n.aplicacion());
        e.setIdempotencyKey(n.idempotencyKey());
        e.setTipo(n.tipo());
        e.setIdioma(n.idioma());
        e.setAsunto(n.asunto());
        e.setMensaje(n.mensaje());
        e.setDestinatarios(json.writeValueAsString(n.destinatarios()));
        e.setDatos(json.writeValueAsString(n.datos()));
        e.setEstado(n.estado());
        e.setIntentos(n.intentos());
        e.setCreadoEn(n.creadoEn());
        e.setProximoIntentoEn(n.proximoIntentoEn());
        e.setIdCorrelacion(n.idCorrelacion());
        e.setTraceparent(n.traceparent());
        return e;
    }

    private Notificacion aDominio(final NotificacionEntity e) {
        return new Notificacion(e.getId(), e.getAplicacion(), e.getIdempotencyKey(), e.getTipo(), e.getIdioma(), e.getAsunto(),
                e.getMensaje(), leer(e.getDestinatarios(), TIPO_DESTINATARIOS, List.of()), leer(e.getDatos(), TIPO_DATOS, Map.of()),
                e.getEstado(), e.getIntentos(), e.getCreadoEn(), e.getProximoIntentoEn(), e.getReservadoHasta(), e.getReservadoPor(),
                e.getEntregadoEn(), e.getFalloEn(), e.getCodigoError(), e.getDetalleError(), e.getIdCorrelacion(), e.getTraceparent());
    }

    private <T> T leer(final String contenido, final TypeReference<T> tipo, final T porDefecto) {
        if (contenido == null || contenido.isBlank()) {
            return porDefecto;
        }
        return json.readValue(contenido, tipo);
    }

    private static String recortar(final String texto, final int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
