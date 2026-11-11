package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Proveedor de {@link ParameterCatalogPort} sobre el componente externo "Catalogo de Parametros".
 *
 * <p>Una sola llamada {@code GET {baseUrl}/tokens/{tokenId}/parametros} trae todos los parametros y se conserva en
 * memoria mientras no venza {@code cacheTtl}. El secreto y la cabecera Authorization nunca se registran ni viajan en
 * mensajes de error. No se reintenta ni se sirven datos vencidos en silencio.</p>
 */
public final class CatalogoParametrosHttpParameterCatalogAdapter
        implements ParameterCatalogPort, InvalidableParameterCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogoParametrosHttpParameterCatalogAdapter.class);
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final HttpClient httpClient;
    private final CatalogoParametrosProviderProperties properties;
    private final URI uri;
    private final long ttlNanos;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Snapshot snapshot;

    public CatalogoParametrosHttpParameterCatalogAdapter(final HttpClient httpClient,
                                                         final CatalogoParametrosProviderProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "El HttpClient es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "Las propiedades del catalogo son obligatorias.");
        this.uri = URI.create(properties.baseUrl() + "/tokens/" + properties.tokenId() + "/parametros");
        this.ttlNanos = properties.cacheTtl().toNanos();
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
        snapshot = null;
    }

    @Override
    public void invalidateAll() {
        snapshot = null;
    }

    private Map<String, String> valores() {
        final Snapshot actual = snapshot;
        if (vigente(actual)) {
            return actual.valores();
        }
        lock.lock();
        try {
            final Snapshot recheck = snapshot;
            if (vigente(recheck)) {
                return recheck.valores();
            }
            try {
                final Map<String, String> cargados = cargar();
                snapshot = new Snapshot(cargados, System.nanoTime());
                return cargados;
            } catch (final ParameterCatalogException exception) {
                if (recheck != null) {
                    LOGGER.warn("Catalogo de Parametros no disponible; la cache vencida no se reutiliza: {}",
                            exception.getMessage());
                }
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean vigente(final Snapshot candidato) {
        return ttlNanos > 0 && candidato != null && System.nanoTime() - candidato.cargadoEn() < ttlNanos;
    }

    private Map<String, String> cargar() {
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.timeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.secreto())
                .GET()
                .build();
        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            throw new ParameterCatalogException("No fue posible consultar el Catalogo de Parametros.", exception);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParameterCatalogException("Consulta al Catalogo de Parametros interrumpida.", exception);
        }
        final int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new ParameterCatalogException("El Catalogo de Parametros respondio HTTP " + status + ".");
        }
        return interpretar(response.body());
    }

    private static Map<String, String> interpretar(final String body) {
        final JsonNode raiz;
        try {
            raiz = JSON.readTree(body);
        } catch (final RuntimeException exception) {
            throw new ParameterCatalogException("Respuesta invalida del Catalogo de Parametros.", exception);
        }
        final Map<String, String> resultado = new HashMap<>();
        final JsonNode parametros = raiz.path("parametros");
        if (!parametros.isArray()) {
            return Map.of();
        }
        for (final JsonNode item : parametros) {
            final JsonNode parametro = item.path("parametro");
            final String nombre = parametro.path("nombre").asString("").trim();
            if (nombre.isEmpty() || !parametro.path("activo").asBoolean(true)) {
                continue;
            }
            for (final JsonNode metadato : item.path("metadatos")) {
                final JsonNode valor = metadato.path("metadato").path("valor");
                if (!valor.isMissingNode() && !valor.isNull()) {
                    resultado.putIfAbsent(nombre, valor.asString());
                    break;
                }
            }
        }
        return Map.copyOf(resultado);
    }

    private record Snapshot(Map<String, String> valores, long cargadoEn) {
    }
}
