package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.application.model.EstadoCanal;
import co.edu.uco.notificaciones.application.model.FalloReciente;
import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResultadoReintento;
import co.edu.uco.notificaciones.application.model.SolicitudNotificacion;
import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.application.port.out.DespertadorPort;
import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.Correos;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.domain.TipoNotificacion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Casos de uso de la cola: recibir avisos (idempotente), consultarla, reintentar, correo de prueba y estado del canal. */
public class GestionNotificaciones implements NotificacionesUseCase {

    public static final String TIPO_PRUEBA_GENERICO = "PRUEBA_CANAL_NOTIFICACION";
    public static final String APLICACION_ADMIN = "notificaciones-admin";
    public static final int MAX_DESTINATARIOS = 200;
    public static final int MAX_DATOS = 100;
    public static final int MAX_VALOR_DATO = 2000;

    private static final String IDIOMA_DEFECTO = "es";
    private static final Pattern APLICACION = Pattern.compile("[A-Za-z0-9_.-]{1,60}");
    private static final Pattern IDEMPOTENCIA = Pattern.compile("[A-Za-z0-9_.:@-]{1,120}");
    private static final Pattern IDIOMA = Pattern.compile("[a-z]{2}");
    private static final Pattern CORRELACION = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Pattern TRACEPARENT = Pattern.compile("[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
    private static final int TAMANO_DEFECTO = 20;
    private static final int TAMANO_MAXIMO = 100;

    private final NotificacionRepositoryPort repositorio;
    private final PlantillaRepositoryPort plantillas;
    private final DespertadorPort despertador;
    private final MetricasPort metricas;
    private final EnviadorCorreo enviador;
    private final ConfiguracionCanal canal;
    private final ConfiguracionDespachador despachador;
    private final Clock clock;

    public GestionNotificaciones(
            final NotificacionRepositoryPort repositorio,
            final PlantillaRepositoryPort plantillas,
            final DespertadorPort despertador,
            final MetricasPort metricas,
            final EnviadorCorreo enviador,
            final ConfiguracionCanal canal,
            final ConfiguracionDespachador despachador,
            final Clock clock
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "NotificacionRepositoryPort es obligatorio.");
        this.plantillas = Objects.requireNonNull(plantillas, "PlantillaRepositoryPort es obligatorio.");
        this.despertador = Objects.requireNonNull(despertador, "DespertadorPort es obligatorio.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasPort es obligatorio.");
        this.enviador = Objects.requireNonNull(enviador, "EnviadorCorreo es obligatorio.");
        this.canal = Objects.requireNonNull(canal, "ConfiguracionCanal es obligatoria.");
        this.despachador = Objects.requireNonNull(despachador, "ConfiguracionDespachador es obligatoria.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public ResultadoRegistro registrar(final SolicitudNotificacion s) {
        Objects.requireNonNull(s, "La solicitud es obligatoria.");
        validarTexto(s.aplicacion(), APLICACION, "APLICACION_INVALIDA", "La aplicacion es obligatoria (hasta 60 caracteres: letras, numeros, . _ -).");
        validarTexto(s.idempotencyKey(), IDEMPOTENCIA, "IDEMPOTENCY_KEY_INVALIDA", "La clave de idempotencia es obligatoria (hasta 120 caracteres).");
        final String tipo = s.tipo() == null ? "" : s.tipo().trim();
        final Optional<TipoNotificacion> tipoConocido = plantillas.buscarTipo(tipo);
        if (tipoConocido.isEmpty() || tipoConocido.get().interno()) {
            throw ErrorNegocio.validacion("TIPO_NO_SOPORTADO", "El tipo de notificacion no esta entre los tipos entregables.");
        }
        final String idioma = s.idioma() == null || s.idioma().isBlank() ? IDIOMA_DEFECTO : s.idioma().trim();
        validarTexto(idioma, IDIOMA, "IDIOMA_INVALIDO", "El idioma debe ser un codigo de dos letras minusculas.");
        final List<Destinatario> destinatarios = validarDestinatarios(s.destinatarios());
        final Map<String, String> datos = validarDatos(s.datos());
        validarOpcional(s.idCorrelacion(), CORRELACION, "CORRELACION_INVALIDA", "El identificador de correlacion no es valido.");
        validarOpcional(s.traceparent(), TRACEPARENT, "TRACEPARENT_INVALIDO", "La cabecera traceparent no tiene el formato W3C.");
        validarLargo(s.asunto(), ValidadorPlantilla.MAX_ASUNTO, "ASUNTO_INVALIDO");
        validarLargo(s.mensaje(), ValidadorPlantilla.MAX_CUERPO, "MENSAJE_INVALIDO");

        final Instant ahora = clock.instant();
        final Notificacion nueva = new Notificacion(UUID.randomUUID(), s.aplicacion().trim(), s.idempotencyKey().trim(), tipo, idioma,
                blancoANulo(s.asunto()), blancoANulo(s.mensaje()), destinatarios, datos, EstadoEnvio.PENDIENTE, 0, ahora, ahora,
                null, null, null, null, null, null, blancoANulo(s.idCorrelacion()), blancoANulo(s.traceparent()));
        final ResultadoRegistro resultado = repositorio.registrarSiNoExiste(nueva);
        metricas.contar("notificaciones.recibidas.total", 1, "resultado", resultado.creada() ? "nueva" : "duplicada");
        if (resultado.creada()) {
            despertador.despertar();
        }
        return resultado;
    }

    @Override
    public PaginaNotificaciones consultar(final FiltroNotificaciones filtro, final int pagina, final int tamano) {
        final FiltroNotificaciones f = filtro == null ? new FiltroNotificaciones(null, null, null) : filtro;
        final String estado = blancoANulo(f.estado());
        if (estado != null) {
            try {
                EstadoEnvio.valueOf(estado);
            } catch (final IllegalArgumentException ex) {
                throw ErrorNegocio.validacion("ESTADO_INVALIDO", "El estado no existe: use PENDIENTE, RESERVADA, FALLIDA_REINTENTABLE, ENTREGADA o FALLIDA_DEFINITIVA.");
            }
        }
        final int numeroPagina = Math.max(1, pagina);
        final int tamanoPagina = tamano <= 0 || tamano > TAMANO_MAXIMO ? TAMANO_DEFECTO : tamano;
        return repositorio.consultar(new FiltroNotificaciones(estado, blancoANulo(f.tipo()), blancoANulo(f.aplicacion())), numeroPagina, tamanoPagina);
    }

    @Override
    public void reintentar(final UUID id) {
        Objects.requireNonNull(id, "El identificador de la notificacion es obligatorio.");
        final ResultadoReintento resultado = repositorio.reencolar(id, clock.instant());
        if (resultado == ResultadoReintento.NO_EXISTE) {
            throw ErrorNegocio.noEncontrado("NOTIFICACION_NO_ENCONTRADA", "La notificacion no existe.");
        }
        if (resultado == ResultadoReintento.ESTADO_INVALIDO) {
            throw ErrorNegocio.conflicto("ESTADO_NO_REINTENTABLE", "Solo se pueden reencolar notificaciones en estado fallido definitivo o reintentable.");
        }
        despertador.despertar();
    }

    @Override
    public Notificacion enviarCorreoPrueba(final String tipo, final String correoDestino, final String solicitadoPor,
                                          final String idCorrelacion) {
        if (!Correos.esCorreoSeguro(correoDestino)) {
            throw ErrorNegocio.validacion("DESTINATARIO_INVALIDO", "El correo de destino no tiene un formato valido.");
        }
        final String tipoEfectivo = tipo == null || tipo.isBlank() ? TIPO_PRUEBA_GENERICO : tipo.trim();
        final Optional<TipoNotificacion> tipoConocido = plantillas.buscarTipo(tipoEfectivo);
        if (tipoConocido.isEmpty()) {
            throw ErrorNegocio.noEncontrado("TIPO_NO_ENCONTRADO", "El tipo de notificacion no existe.");
        }
        // Las muestras salen de los ejemplos que el servicio declara por marcador: la prueba nunca emite datos reales.
        final Map<String, String> datos = new LinkedHashMap<>(MuestrasNotificacion.completo());
        for (final Marcador m : plantillas.marcadores(tipoEfectivo)) {
            datos.put(m.marcador(), m.ejemplo() == null || m.ejemplo().isBlank() ? MuestrasNotificacion.valorPara(m.marcador()) : m.ejemplo());
        }
        // Quien pidio la prueba va en los datos; la correlacion es la de la peticion (se genera solo si no llego).
        final String solicitante = blancoANulo(solicitadoPor);
        if (solicitante != null) {
            datos.put("solicitadoPor", solicitante);
        }
        final String correlacion = blancoANulo(idCorrelacion) != null ? idCorrelacion.trim() : UUID.randomUUID().toString();
        final Instant ahora = clock.instant();
        final Notificacion prueba = new Notificacion(UUID.randomUUID(), APLICACION_ADMIN, "prueba-" + UUID.randomUUID(), tipoEfectivo,
                IDIOMA_DEFECTO,
                "Prueba de canal de notificación institucional (" + tipoEfectivo + ")",
                "Este es un mensaje de prueba emitido desde el panel de administración institucional del Sistema de Gestión de Asistencias UCO.",
                List.of(new Destinatario(correoDestino.trim(), "Administrador Institucional")), datos, EstadoEnvio.PENDIENTE, 0, ahora, ahora,
                null, null, null, null, null, null, correlacion, null);
        final ResultadoRegistro resultado = repositorio.registrarSiNoExiste(prueba);
        despertador.despertar();
        return resultado.notificacion();
    }

    @Override
    public EstadoCanal estadoCanal() {
        final boolean disponible = enviador.disponible();
        final Optional<Instant> ultimoEnvio = repositorio.ultimaEntrega();
        final Optional<FalloReciente> fallo = repositorio.ultimoFallo();
        final String ultimoError = fallo
                .filter(f -> ultimoEnvio.isEmpty() || f.en().isAfter(ultimoEnvio.get()))
                .map(FalloReciente::codigo)
                .orElse(null);
        return new EstadoCanal(
                despachador.habilitado() && disponible,
                despachador.habilitado(),
                disponible,
                canal.canal(),
                canal.host(),
                canal.port(),
                canal.username(),
                canal.tienePassword(),
                canal.fromAddress(),
                canal.fromName(),
                canal.starttls(),
                canal.ssl(),
                canal.maxBcc(),
                canal.ratePerMinute(),
                canal.ratePerDay(),
                canal.platformUrl(),
                canal.testModeEnabled(),
                canal.testModeRedirectTo().size(),
                ultimoEnvio.orElse(null),
                ultimoError);
    }

    private static List<Destinatario> validarDestinatarios(final List<Destinatario> origen) {
        if (origen == null || origen.isEmpty()) {
            throw ErrorNegocio.validacion("DESTINATARIOS_OBLIGATORIOS", "Se requiere al menos un destinatario.");
        }
        if (origen.size() > MAX_DESTINATARIOS) {
            throw ErrorNegocio.validacion("DESTINATARIOS_EXCEDIDOS", "Se admiten hasta " + MAX_DESTINATARIOS + " destinatarios por notificacion.");
        }
        final List<Destinatario> limpios = new ArrayList<>();
        for (final Destinatario d : origen) {
            if (d == null || !Correos.esCorreoSeguro(d.correo())) {
                throw ErrorNegocio.validacion("DESTINATARIO_INVALIDO", "Un destinatario no tiene un correo con formato valido.");
            }
            final String nombre = d.nombre() == null || d.nombre().isBlank() ? null : d.nombre().trim();
            if (nombre != null && nombre.length() > 200) {
                throw ErrorNegocio.validacion("DESTINATARIO_INVALIDO", "El nombre de un destinatario excede 200 caracteres.");
            }
            limpios.add(new Destinatario(d.correo().trim(), nombre));
        }
        return limpios;
    }

    private static Map<String, String> validarDatos(final Map<String, String> origen) {
        if (origen == null) {
            return Map.of();
        }
        if (origen.size() > MAX_DATOS) {
            throw ErrorNegocio.validacion("DATOS_EXCEDIDOS", "Se admiten hasta " + MAX_DATOS + " datos por notificacion.");
        }
        final Map<String, String> limpios = new LinkedHashMap<>();
        origen.forEach((clave, valor) -> {
            if (clave == null || !clave.matches("[a-zA-Z][a-zA-Z0-9]{0,39}")) {
                throw ErrorNegocio.validacion("DATO_INVALIDO", "Los nombres de los datos deben ser alfanumericos (hasta 40 caracteres).");
            }
            if (valor != null && valor.length() > MAX_VALOR_DATO) {
                throw ErrorNegocio.validacion("DATO_INVALIDO", "Un valor de datos excede " + MAX_VALOR_DATO + " caracteres.");
            }
            if (valor != null) {
                limpios.put(clave, valor);
            }
        });
        return limpios;
    }

    private static void validarTexto(final String valor, final Pattern patron, final String codigo, final String mensaje) {
        if (valor == null || !patron.matcher(valor.trim()).matches()) {
            throw ErrorNegocio.validacion(codigo, mensaje);
        }
    }

    private static void validarOpcional(final String valor, final Pattern patron, final String codigo, final String mensaje) {
        if (valor != null && !valor.isBlank() && !patron.matcher(valor.trim()).matches()) {
            throw ErrorNegocio.validacion(codigo, mensaje);
        }
    }

    private static void validarLargo(final String valor, final int max, final String codigo) {
        if (valor != null && valor.length() > max) {
            throw ErrorNegocio.validacion(codigo, "El texto excede " + max + " caracteres.");
        }
    }

    private static String blancoANulo(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
