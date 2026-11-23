package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Conexion con el Servicio de Notificaciones UCO (F4). La URL vacia significa "servicio no configurado": el canal
 * {@code servicio} no se activa y la administracion responde 503. La clave de API es secreta: nunca se imprime.
 *
 * @param url                   base del servicio (p. ej. {@code http://ext-notificaciones-servicio:8080}), sin barra final
 * @param apiKey                valor de la cabecera {@code X-Api-Key}
 * @param connectTimeout        tiempo maximo para abrir la conexion
 * @param readTimeout           tiempo maximo de toda la peticion (respuesta incluida)
 * @param aplicacion            identificador de esta aplicacion ante el servicio
 * @param idioma                idioma por defecto de los avisos
 * @param requireConfirmedEmail si es true, solo se envia a correos confirmados (excepto verificacion y activacion)
 * @param platformUrl           URL publica del frontend, usada para armar los enlaces de verificacion y activacion
 */
@ConfigurationProperties(prefix = "asistencias.notificaciones.service")
public record NotificacionesServiceProperties(
        @DefaultValue("") String url,
        @DefaultValue("") String apiKey,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        @DefaultValue("asistencias") String aplicacion,
        @DefaultValue("es") String idioma,
        @DefaultValue("false") boolean requireConfirmedEmail,
        @DefaultValue("") String platformUrl
) {

    @ConstructorBinding
    public NotificacionesServiceProperties {
        url = validarUrl(url, "url");
        apiKey = apiKey == null ? "" : apiKey.strip();
        connectTimeout = validarDuracion(connectTimeout, Duration.ofSeconds(2), "connect-timeout");
        readTimeout = validarDuracion(readTimeout, Duration.ofSeconds(10), "read-timeout");
        aplicacion = aplicacion == null || aplicacion.isBlank() ? "asistencias" : aplicacion.strip();
        idioma = idioma == null || idioma.isBlank() ? "es" : idioma.strip();
        platformUrl = validarUrl(platformUrl, "platform-url");
    }

    /** {@code true} cuando hay una URL del servicio configurada. */
    public boolean configurado() {
        return !url.isEmpty();
    }

    private static String validarUrl(final String valor, final String nombre) {
        final String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            return "";
        }
        if (!limpio.startsWith("http://") && !limpio.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "asistencias.notificaciones.service." + nombre + " debe iniciar con http:// o https://.");
        }
        return limpio.endsWith("/") ? limpio.substring(0, limpio.length() - 1) : limpio;
    }

    private static Duration validarDuracion(final Duration valor, final Duration porDefecto, final String nombre) {
        final Duration efectivo = valor == null ? porDefecto : valor;
        if (efectivo.isZero() || efectivo.isNegative()) {
            throw new IllegalArgumentException(
                    "asistencias.notificaciones.service." + nombre + " debe ser mayor que cero.");
        }
        return efectivo;
    }

    @Override
    public String toString() {
        return "NotificacionesServiceProperties[url=" + url + ", apiKey=[PROTECTED], connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + ", aplicacion=" + aplicacion + ", idioma=" + idioma
                + ", requireConfirmedEmail=" + requireConfirmedEmail + ", platformUrl=" + platformUrl + "]";
    }
}
