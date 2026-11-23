package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Proveedor de {@link ParameterCatalogPort} sobre el componente externo "Catalogo de Parametros".
 *
 * <p>INT-004: lee por M2M ({@code client_credentials} contra Security Baseline y luego {@code GET {baseUrl}/parametros/consulta/m2m}).
 * Una sola llamada trae todos los parametros del alcance de la identidad y se conserva en
 * memoria mientras no venza {@code cacheTtl}. La primera carga es sincrona (si falla, propaga
 * {@link ParameterCatalogException} y el decorador DEC-018 aplica el valor por defecto; durante la misma gracia que el
 * refresco, las lecturas siguientes fallan al instante sin volver a llamar). Cuando la cache vence se siguen
 * sirviendo los valores vencidos y se refresca en segundo plano con una sola peticion en vuelo: una lectura nunca espera
 * por el componente despues de la primera carga. Si el refresco falla se conservan los valores vencidos, se escribe un
 * WARN y no se reintenta hasta pasada una gracia ({@link #GRACIA_MAXIMA_TRAS_FALLO}, acotada por el TTL), de modo que un
 * Catalogo lento o caido no degrada cada lectura a los valores por defecto del codigo ni llena el log. El refresco en segundo plano
 * no espera a Keycloak: si no hay token guardado lo pide en otro hilo. Con
 * {@code cacheTtl = 0} cada lectura consulta al componente. {@link #invalidateAll()} descarta la cache y la siguiente
 * lectura vuelve a cargar de forma sincrona. El secreto, el token y la cabecera Authorization nunca se registran ni viajan en
 * mensajes de error.</p>
 */
public final class CatalogoParametrosHttpParameterCatalogAdapter
        implements ParameterCatalogPort, InvalidableParameterCatalog {

    /** Tras un refresco fallido no se vuelve a intentar antes de este lapso (o del TTL, si es menor). */
    static final Duration GRACIA_MAXIMA_TRAS_FALLO = Duration.ofSeconds(30);

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoParametrosHttpParameterCatalogAdapter.class);
    private static final Executor HILOS_VIRTUALES = Executors.newVirtualThreadPerTaskExecutor();

    private final HttpClient httpClient;
    private final Clock clock;
    private final CatalogoParametrosProviderProperties properties;
    private final CatalogoParametrosM2mClient cliente;
    private final long ttlNanos;
    private final long graciaNanos;
    private final ReentrantLock lock = new ReentrantLock();
    private final CatalogoParametrosTokenProvider tokenProvider;
    private final AtomicBoolean refrescando = new AtomicBoolean();
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();
    private volatile long reintentarDesde;

    public CatalogoParametrosHttpParameterCatalogAdapter(final HttpClient httpClient,
                                                         final CatalogoParametrosProviderProperties properties,
                                                         final CatalogoParametrosTokenProvider tokenProvider) {
        this(httpClient, properties, tokenProvider, Clock.systemUTC());
    }

    /** Constructor con fuente de tiempo inyectable (pruebas deterministas del TTL y la gracia). */
    public CatalogoParametrosHttpParameterCatalogAdapter(final HttpClient httpClient,
                                                         final CatalogoParametrosProviderProperties properties,
                                                         final CatalogoParametrosTokenProvider tokenProvider,
                                                         final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio.");
        this.httpClient = Objects.requireNonNull(httpClient, "El HttpClient es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "Las propiedades del catalogo son obligatorias.");
        this.cliente = new CatalogoParametrosM2mClient(httpClient, properties, tokenProvider);
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "El proveedor de token es obligatorio.");
        this.ttlNanos = properties.cacheTtl().toNanos();
        this.graciaNanos = Math.min(ttlNanos, GRACIA_MAXIMA_TRAS_FALLO.toNanos());
        this.reintentarDesde = ahora();
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || key == null || group.isBlank() || key.isBlank()) {
            return Optional.empty();
        }
        final Map<String, String> valores = valores();
        final String nombre = group.trim() + properties.separador() + key.trim();
        final String exacto = valores.get(nombre);
        if (exacto != null) {
            return Optional.of(exacto);
        }
        return Optional.ofNullable(valores.get(nombre.toUpperCase()));
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key).orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String rawValue = getRequiredParameter(group, key);
        if (targetType == String.class) {
            return (T) rawValue;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(rawValue.trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(rawValue.trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(rawValue.trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(rawValue.trim());
        }
        throw new IllegalArgumentException("Tipo no soportado: " + targetType.getName());
    }

    /** La respuesta del componente es un unico bloque: invalidar un parametro vacia toda la cache. */
    @Override
    public void invalidateParameter(final String group, final String key) {
        snapshot.set(null);
    }

    @Override
    public void invalidateAll() {
        snapshot.set(null);
    }

    private long ahora() {
        final Instant instante = clock.instant();
        return instante.getEpochSecond() * 1_000_000_000L + instante.getNano();
    }

    private Map<String, String> valores() {
        final Snapshot actual = snapshot.get();
        if (vigente(actual)) {
            return actual.valores();
        }
        if (actual != null && ttlNanos > 0) {
            refrescarEnSegundoPlano();
            return actual.valores();
        }
        return cargarBloqueando();
    }

    private boolean vigente(final Snapshot candidato) {
        return ttlNanos > 0 && candidato != null && ahora() - candidato.cargadoEn() < ttlNanos;
    }

    /**
     * Primera carga (o sin cache): una sola peticion aunque lleguen lecturas concurrentes; un fallo se propaga. Tras un
     * fallo, durante la gracia las lecturas fallan al instante sin volver a llamar (el decorador DEC-018 aplica el valor
     * por defecto): asi un Catalogo caido al arrancar no hace esperar el timeout a cada lectura, en serie por el lock.
     */
    private Map<String, String> cargarBloqueando() {
        lock.lock();
        try {
            final Snapshot recheck = snapshot.get();
            if (vigente(recheck)) {
                return recheck.valores();
            }
            if (ahora() - reintentarDesde < 0) {
                throw new ParameterCatalogException(
                        "Catalogo de Parametros no disponible; la carga se reintentara al terminar la gracia.");
            }
            try {
                final Map<String, String> cargados = cliente.interpretar(cliente.enviar(null));
                snapshot.set(new Snapshot(cargados, ahora()));
                return cargados;
            } catch (final ParameterCatalogException exception) {
                registrarFalloDeCarga(exception.getMessage());
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    private void registrarFalloDeCarga(final String detalle) {
        reintentarDesde = ahora() + graciaNanos;
        LOGGER.warn("Catalogo de Parametros no disponible en la carga inicial; se aplican los valores por defecto y no se "
                + "reintenta durante {} s. causa={}", graciaNanos / 1_000_000_000L, detalle);
    }

    /** Cache vencida: un solo refresco en vuelo; mientras tanto (y si falla) se sirven los valores vencidos. */
    private void refrescarEnSegundoPlano() {
        if (ahora() - reintentarDesde < 0 || !refrescando.compareAndSet(false, true)) {
            return;
        }
        final CompletableFuture<HttpResponse<String>> envio;
        try {
            envio = enviarAsincrono();
        } catch (final RuntimeException exception) {
            registrarFalloDeRefresco(exception.getClass().getSimpleName());
            refrescando.set(false);
            return;
        }
        envio.whenComplete((response, error) -> {
            try {
                if (error != null) {
                    registrarFalloDeRefresco(causa(error).getClass().getSimpleName());
                    return;
                }
                snapshot.set(new Snapshot(cliente.interpretar(response), ahora()));
            } catch (final ParameterCatalogException exception) {
                registrarFalloDeRefresco(exception.getMessage());
            } finally {
                refrescando.set(false);
            }
        });
    }

    private void registrarFalloDeRefresco(final String detalle) {
        reintentarDesde = ahora() + graciaNanos;
        LOGGER.warn("Catalogo de Parametros no disponible; se conservan los valores vencidos y no se reintenta "
                + "durante {} s. causa={}", graciaNanos / 1_000_000_000L, detalle);
    }

    private static Throwable causa(final Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    /** Con token guardado envia al instante; sin el, lo pide en un hilo virtual para que la lectura no espere a Keycloak. */
    private CompletableFuture<HttpResponse<String>> enviarAsincrono() {
        final var handler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        final Optional<String> token = tokenProvider.tokenEnCache();
        if (token.isPresent()) {
            return httpClient.sendAsync(cliente.peticion(null, token.get()), handler);
        }
        return CompletableFuture.supplyAsync(() -> cliente.peticion(null), HILOS_VIRTUALES)
                .thenCompose(peticion -> httpClient.sendAsync(peticion, handler));
    }

    private record Snapshot(Map<String, String> valores, long cargadoEn) {
    }
}
