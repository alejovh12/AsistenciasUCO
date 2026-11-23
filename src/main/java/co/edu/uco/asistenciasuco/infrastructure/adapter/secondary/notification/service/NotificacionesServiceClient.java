package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cliente HTTP del Servicio de Notificaciones UCO (contrato {@code openapi-notificaciones.yaml}). Unico punto del backend que
 * habla con el servicio: el resto del modulo (canal de salida del outbox y administracion) lo usa y no conoce HTTP.
 *
 * <p>Cada llamada envia {@code X-Api-Key}, {@code X-Correlation-Id} y {@code traceparent}. El circuit breaker cuenta como falla
 * los errores de red, los tiempos de espera y los 5xx; un 4xx es una respuesta valida del servicio y no abre el breaker. Una
 * respuesta fuera de 2xx se traduce a {@link ServicioNotificacionesException}; la imposibilidad de llegar al servicio, a
 * {@link ServicioNotificacionesNoDisponibleException}. Nunca registra la clave, los correos ni los cuerpos.</p>
 */
public final class NotificacionesServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionesServiceClient.class);

    /**
     * Rutas que el backend puede pedir al servicio: siempre bajo {@code /api/v1}, con segmentos ya codificados
     * ({@link #codificar(String)}) y sin {@code .} ni {@code ..} como segmento. Asi un valor del usuario (tipo de plantilla,
     * version, id) no puede cambiar el destino de la peticion (SSRF).
     */
    private static final Pattern RUTA_PERMITIDA = Pattern.compile("^/api/v1(?!.*/\\.{1,2}(?:/|$))(?:/[A-Za-z0-9._~*%-]+)+$");
    /** Consulta ya codificada por {@link #consultaCodificada(Map)}: vacia o {@code ?clave=valor&...}. */
    private static final Pattern CONSULTA_PERMITIDA = Pattern.compile("^(?:\\?[A-Za-z0-9._~*%=&-]*)?$");

    private final HttpClient httpClient;
    private final NotificacionesServiceProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    public NotificacionesServiceClient(
            final HttpClient httpClient,
            final NotificacionesServiceProperties properties,
            final CircuitBreaker circuitBreaker,
            final ObjectMapper objectMapper
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "El HttpClient es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "NotificacionesServiceProperties es obligatorio.");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "El circuit breaker del servicio es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "El ObjectMapper es obligatorio.");
    }

    /** {@code true} cuando hay una URL configurada. */
    public boolean configurado() {
        return properties.configurado();
    }

    /** {@code false} mientras el circuit breaker esta abierto: el despachador no consume intentos de las notificaciones. */
    public boolean disponible() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    public RespuestaServicio get(final String ruta, final Map<String, String> consulta) {
        return ejecutar(solicitud(ruta, consulta).GET());
    }

    public RespuestaServicio post(final String ruta, final Object cuerpo) {
        return post(ruta, Map.of(), cuerpo);
    }

    public RespuestaServicio post(final String ruta, final Map<String, String> consulta, final Object cuerpo) {
        final String json;
        try {
            json = cuerpo == null ? "" : objectMapper.writeValueAsString(cuerpo);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar la solicitud al servicio de notificaciones.", exception);
        }
        return ejecutar(solicitud(ruta, consulta)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    /**
     * Salud del servicio ({@code GET /actuator/health/readiness}, abierta). No pasa por el circuit breaker: una consulta de
     * salud no debe abrirlo ni cerrarlo.
     */
    public boolean readiness() {
        if (!properties.configurado()) {
            return false;
        }
        try {
            final HttpResponse<String> respuesta = httpClient.send(
                    HttpRequest.newBuilder(URI.create(properties.url() + "/actuator/health/readiness"))
                            .timeout(properties.readTimeout()).header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return respuesta.statusCode() == 200 && "UP".equals(leer(respuesta.body()).path("status").asText());
        } catch (final IOException | RuntimeException _) {
            return false;
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private HttpRequest.Builder solicitud(final String ruta, final Map<String, String> consulta) {
        if (!properties.configurado()) {
            throw new ServicioNotificacionesNoDisponibleException("El servicio de notificaciones no esta configurado.");
        }
        final HttpRequest.Builder builder = HttpRequest.newBuilder(destino(ruta, consultaCodificada(consulta)))
                .timeout(properties.readTimeout())
                .header("Accept", "application/json")
                .header("X-Api-Key", properties.apiKey());
        final var correlacion = CorrelationIdContext.get();
        if (correlacion != null) {
            builder.header("X-Correlation-Id", correlacion.toString());
        }
        final String traceparent = traceparentActual();
        if (traceparent != null) {
            builder.header("traceparent", traceparent);
        }
        return builder;
    }

    /**
     * Construye la URI de destino solo con rutas permitidas: la URL base (host y puerto) sale de la configuracion y lo que
     * llega del usuario queda confinado a segmentos codificados bajo {@code /api/v1}. Otra ruta es un error de programacion
     * y no llega a la red.
     */
    private URI destino(final String ruta, final String consulta) {
        if (ruta == null || !RUTA_PERMITIDA.matcher(ruta).matches() || !CONSULTA_PERMITIDA.matcher(consulta).matches()) {
            throw new IllegalArgumentException("Ruta no permitida hacia el servicio de notificaciones.");
        }
        return URI.create(properties.url() + ruta + consulta);
    }

    private RespuestaServicio ejecutar(final HttpRequest.Builder builder) {
        if (!circuitBreaker.tryAcquirePermission()) {
            throw new ServicioNotificacionesNoDisponibleException("Servicio de notificaciones con circuit breaker abierto.");
        }
        final long inicio = circuitBreaker.getCurrentTimestamp();
        final HttpRequest peticion = builder.build();
        final HttpResponse<String> respuesta;
        try {
            respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (final HttpTimeoutException exception) {
            registrarError(inicio, exception);
            throw new ServicioNotificacionesNoDisponibleException("Tiempo de espera agotado con el servicio de notificaciones.", exception);
        } catch (final IOException exception) {
            registrarError(inicio, exception);
            throw new ServicioNotificacionesNoDisponibleException("No fue posible conectar con el servicio de notificaciones.", exception);
        } catch (final InterruptedException exception) {
            circuitBreaker.releasePermission();
            Thread.currentThread().interrupt();
            throw new ServicioNotificacionesNoDisponibleException("Llamada al servicio de notificaciones interrumpida.", exception);
        } catch (final RuntimeException exception) {
            registrarError(inicio, exception);
            throw new ServicioNotificacionesNoDisponibleException("Fallo inesperado llamando al servicio de notificaciones.", exception);
        }

        final int status = respuesta.statusCode();
        if (status >= 500) {
            registrarError(inicio, new IllegalStateException("HTTP " + status));
        } else {
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - inicio, circuitBreaker.getTimestampUnit());
        }
        if (status >= 200 && status < 300) {
            return new RespuestaServicio(status, leer(respuesta.body()));
        }
        final JsonNode cuerpoError = leer(respuesta.body());
        throw new ServicioNotificacionesException(status, cuerpoError.path("code").asText(""), cuerpoError.path("message").asText(""));
    }

    private void registrarError(final long inicio, final Throwable causa) {
        circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - inicio, circuitBreaker.getTimestampUnit(), causa);
        LOGGER.warn("Falla del servicio de notificaciones: causa={}", causa.getClass().getSimpleName());
    }

    private JsonNode leer(final String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return objectMapper.readTree(cuerpo);
        } catch (final JsonProcessingException _) {
            return MissingNode.getInstance();
        }
    }

    private static String consultaCodificada(final Map<String, String> consulta) {
        if (consulta == null || consulta.isEmpty()) {
            return "";
        }
        return consulta.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> codificar(e.getKey()) + "=" + codificar(e.getValue()))
                .collect(Collectors.joining("&", "?", ""));
    }

    /** Codifica un segmento de ruta o un valor de consulta (espacio como %20). */
    public static String codificar(final String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String traceparentActual() {
        final SpanContext contexto = Span.current().getSpanContext();
        if (!contexto.isValid()) {
            return null;
        }
        return "00-" + contexto.getTraceId() + "-" + contexto.getSpanId() + "-" + (contexto.isSampled() ? "01" : "00");
    }
}
