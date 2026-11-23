package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.SincronizarParametrosCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort.ParametroExterno;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.Operacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.ResultadoSincronizacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * INT-004: refleja en la cache y en {@code dbo.CatalogoParametro} los cambios del Catalogo de Parametros externo.
 *
 * <ul>
 *   <li>{@code Parameter*}, {@code Metadata*} y {@code MetadataEnvironment*} (Catalogo 0.1.17: el valor vive en el metadato, asi que
 *       un cambio de valor llega como {@code MetadataUpdated} con {@code resourceName} = nombre del parametro): el evento solo
 *       notifica; se invalida la cache, se relee el parametro por M2M (nunca se usa {@code resourceValue}) y se hace {@code UPSERT}
 *       en la BD. Si el Catalogo ya no devuelve un valor (metadato borrado, parametro inactivo, valor vacio) la BD CONSERVA su valor
 *       local: una lectura vacia no basta para pisarlo. Un metadato sin nombre usable reconcilia el catalogo completo.</li>
 *   <li>Solo {@code ParameterDeleted} con el parametro ausente del Catalogo hace {@code DELETE}: la fila se conserva y vuelve a su
 *       valor por defecto (los SP la leen). Si todavia lo devuelve, manda el Catalogo ({@code UPSERT}).</li>
 *   <li>Un evento ajeno (aplicacion o ambiente distintos de los configurados), de un tipo desconocido o sin nombre usable se
 *       ignora. Un evento repetido (mismo {@code eventId}) tambien; se recuerda solo si se proceso sin error, para que el
 *       broker lo reentregue si fallo el Catalogo o la BD. La BD tambien es idempotente por {@code eventId}.</li>
 * </ul>
 */
public class SincronizarParametrosCatalogoUseCaseImpl implements SincronizarParametrosCatalogoUseCase {

    static final String PREFIJO_PARAMETRO = "Parameter";
    static final String PREFIJO_METADATO = "Metadata";
    static final String SUFIJO_BORRADO = "Deleted";
    static final String SUFIJO_CREADO = "Created";
    static final String SUFIJO_ACTUALIZADO = "Updated";
    static final int MAX_AVISOS_RECORDADOS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizarParametrosCatalogoUseCaseImpl.class);

    private final ParameterCatalogSourcePort fuente;
    private final ParametroCatalogoRepositoryPort repositorio;
    private final CatalogInvalidationPort invalidacion;
    private final String separador;
    private final String applicationId;
    private final String environmentId;
    private final Clock clock;
    private final Set<String> avisosProcesados = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_AVISOS_RECORDADOS;
                }
            }));

    public SincronizarParametrosCatalogoUseCaseImpl(
            final ParameterCatalogSourcePort fuente,
            final ParametroCatalogoRepositoryPort repositorio,
            final CatalogInvalidationPort invalidacion,
            final String separador,
            final String applicationId,
            final String environmentId,
            final Clock clock
    ) {
        this.fuente = Objects.requireNonNull(fuente, "ParameterCatalogSourcePort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "ParametroCatalogoRepositoryPort es obligatorio.");
        this.invalidacion = Objects.requireNonNull(invalidacion, "CatalogInvalidationPort es obligatorio.");
        this.separador = separador == null || separador.isEmpty() ? "." : separador;
        this.applicationId = vacio(applicationId) ? null : applicationId.trim();
        this.environmentId = vacio(environmentId) ? null : environmentId.trim();
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public ResumenSincronizacion procesarAviso(final AvisoCambioParametro aviso) {
        if (aviso == null || vacio(aviso.tipo())) {
            return ResumenSincronizacion.vacio();
        }
        if (!esDeEsteAlcance(aviso)) {
            LOGGER.debug("Evento del Catalogo de Parametros de otra aplicacion o ambiente; se ignora. idAviso={}", aviso.idAviso());
            return ResumenSincronizacion.vacio();
        }
        final String idAviso = vacio(aviso.idAviso()) ? null : aviso.idAviso().trim();
        if (idAviso != null && avisosProcesados.contains(idAviso)) {
            LOGGER.debug("Evento del Catalogo de Parametros repetido; se ignora. idAviso={}", idAviso);
            return ResumenSincronizacion.vacio();
        }
        final ResumenSincronizacion resumen = despachar(aviso, idAviso);
        if (idAviso != null) {
            avisosProcesados.add(idAviso);
        }
        return resumen;
    }

    @Override
    public ResumenSincronizacion reconciliarTodo() {
        invalidacion.invalidateAll();
        final List<ParametroExterno> parametros = fuente.listar();
        // La hora de la lectura (ya terminada) es la que ordena la reconciliacion frente a los eventos.
        final Instant leidoEn = clock.instant();
        int aplicados = 0;
        int sinCambios = 0;
        int desconocidos = 0;
        int obsoletos = 0;
        int rechazados = 0;
        int fallidos = 0;
        for (final ParametroExterno parametro : parametros) {
            try {
                switch (repositorio.sincronizarParametroExterno(parametro.grupo(), parametro.clave(), parametro.valor(),
                        Operacion.UPSERT, null, leidoEn)) {
                    case APLICADO -> aplicados++;
                    case SIN_CAMBIOS, DUPLICADO -> sinCambios++;
                    case DESCONOCIDO -> desconocidos++;
                    case OBSOLETO -> obsoletos++;
                    case RECHAZADO -> rechazados++;
                }
            } catch (final RuntimeException e) {
                fallidos++;
                LOGGER.warn("No se pudo sincronizar el parametro en la BD. grupo={}, clave={}, causa={}",
                        parametro.grupo(), parametro.clave(), e.getClass().getSimpleName());
            }
        }
        final ResumenSincronizacion resumen = new ResumenSincronizacion(parametros.size(), aplicados, sinCambios,
                desconocidos, obsoletos, rechazados, fallidos);
        LOGGER.info("Reconciliacion de parametros con el Catalogo de Parametros: {}", resumen);
        return resumen;
    }

    private ResumenSincronizacion despachar(final AvisoCambioParametro aviso, final String idAviso) {
        final String tipo = aviso.tipo().trim();
        final boolean metadato = tipo.startsWith(PREFIJO_METADATO);
        final boolean parametroBorrado = tipo.equals(PREFIJO_PARAMETRO + SUFIJO_BORRADO);
        if (!esTipoConocido(tipo)) {
            LOGGER.debug("Tipo de evento del Catalogo de Parametros desconocido; se ignora. tipo={}", tipo);
            return ResumenSincronizacion.vacio();
        }
        final String nombre = vacio(aviso.nombreRecurso()) ? null : aviso.nombreRecurso().trim();
        final int corte = nombre == null ? -1 : nombre.indexOf(separador);
        if (corte <= 0 || corte + separador.length() >= nombre.length()) {
            if (metadato) {
                // El evento no dice a que parametro afecta: una sola lectura M2M completa lo cubre.
                return reconciliarPorEvento(idAviso);
            }
            LOGGER.debug("El evento no trae un nombre GRUPO{}CLAVE; se ignora. tipo={}", separador, tipo);
            return ResumenSincronizacion.vacio();
        }
        invalidacion.invalidateAll();
        final Optional<ParametroExterno> vigente = fuente.obtener(nombre);
        final Instant ocurridoEn = aviso.ocurridoEn() == null ? clock.instant() : aviso.ocurridoEn();
        if (vigente.isPresent()) {
            final ParametroExterno parametro = vigente.get();
            return resumenDe(repositorio.sincronizarParametroExterno(parametro.grupo(), parametro.clave(), parametro.valor(),
                    Operacion.UPSERT, idAviso, ocurridoEn));
        }
        if (!parametroBorrado) {
            LOGGER.warn("El Catalogo de Parametros ya no devuelve el parametro del evento; solo se invalida la cache. tipo={}, nombre={}",
                    tipo, nombre);
            return ResumenSincronizacion.vacio();
        }
        final String grupo = nombre.substring(0, corte);
        final String clave = nombre.substring(corte + separador.length());
        return resumenDe(repositorio.sincronizarParametroExterno(grupo, clave, null, Operacion.DELETE, idAviso, ocurridoEn));
    }

    private ResumenSincronizacion reconciliarPorEvento(final String idAviso) {
        final ResumenSincronizacion resumen = reconciliarTodo();
        if (resumen.fallidos() > 0) {
            throw new IllegalStateException("La BD no pudo guardar " + resumen.fallidos()
                    + " parametros durante la reconciliacion del evento " + idAviso + "; se reintenta.");
        }
        return resumen;
    }

    /** {@code Parameter*}, {@code Metadata*} y {@code MetadataEnvironment*} con sufijo Created, Updated o Deleted. */
    private static boolean esTipoConocido(final String tipo) {
        final String base = tipo.startsWith(PREFIJO_METADATO) ? PREFIJO_METADATO
                : tipo.startsWith(PREFIJO_PARAMETRO) ? PREFIJO_PARAMETRO : null;
        if (base == null) {
            return false;
        }
        final String resto = tipo.substring(base.length());
        final String sufijo = resto.startsWith("Environment") ? resto.substring("Environment".length()) : resto;
        return sufijo.equals(SUFIJO_CREADO) || sufijo.equals(SUFIJO_ACTUALIZADO) || sufijo.equals(SUFIJO_BORRADO);
    }

    private static ResumenSincronizacion resumenDe(final ResultadoSincronizacion resultado) {
        return switch (resultado) {
            case APLICADO -> new ResumenSincronizacion(1, 1, 0, 0, 0, 0, 0);
            case SIN_CAMBIOS, DUPLICADO -> new ResumenSincronizacion(1, 0, 1, 0, 0, 0, 0);
            case DESCONOCIDO -> new ResumenSincronizacion(1, 0, 0, 1, 0, 0, 0);
            case OBSOLETO -> new ResumenSincronizacion(1, 0, 0, 0, 1, 0, 0);
            case RECHAZADO -> new ResumenSincronizacion(1, 0, 0, 0, 0, 1, 0);
        };
    }

    /** Un evento sin ids se acepta (el topic ya es de la identidad); con ids, deben coincidir con los configurados. */
    private boolean esDeEsteAlcance(final AvisoCambioParametro aviso) {
        return coincide(applicationId, aviso.applicationId()) && coincide(environmentId, aviso.environmentId());
    }

    private static boolean coincide(final String esperado, final String recibido) {
        return esperado == null || vacio(recibido) || esperado.equalsIgnoreCase(recibido.trim());
    }

    private static boolean vacio(final String valor) {
        return valor == null || valor.isBlank();
    }
}
