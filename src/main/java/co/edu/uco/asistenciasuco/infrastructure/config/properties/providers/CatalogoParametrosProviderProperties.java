package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.UUID;

/**
 * Como se conecta Asistencias al componente externo "Catalogo de Parametros" (solo aplica con
 * {@code app.adapters.parameter-catalog.provider=catalogoparametros}).
 *
 * @param baseUrl       URL base de la API (sin barra final), p. ej. {@code http://localhost:8081/catalogo-parametros/api/v1}.
 * @param tokenId       identificador del token de consumo; el token decide aplicacion y ambiente.
 * @param secreto       secreto Bearer del token. Nunca se registra en logs ni en mensajes de error.
 * @param applicationId aplicacion (opcional, solo trazabilidad).
 * @param environmentId ambiente (opcional, solo trazabilidad).
 * @param timeout       espera maxima por la llamada HTTP.
 * @param cacheTtl      vigencia de la cache en memoria; {@code 0} la desactiva.
 * @param separador     separador entre grupo y clave dentro de {@code parametro.nombre}.
 */
@ConfigurationProperties(prefix = "app.providers.catalogo-parametros")
public record CatalogoParametrosProviderProperties(
        String baseUrl,
        UUID tokenId,
        String secreto,
        String applicationId,
        String environmentId,
        Duration timeout,
        Duration cacheTtl,
        String separador
) {

    public CatalogoParametrosProviderProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.base-url es obligatorio.");
        }
        baseUrl = baseUrl.strip();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.isEmpty()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.base-url es obligatorio.");
        }
        if (tokenId == null) {
            throw new IllegalStateException("app.providers.catalogo-parametros.token-id es obligatorio.");
        }
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.secreto es obligatorio.");
        }
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.timeout debe ser positivo.");
        }
        cacheTtl = cacheTtl == null ? Duration.ofMinutes(5) : cacheTtl;
        if (cacheTtl.isNegative()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.cache-ttl no puede ser negativo.");
        }
        separador = separador == null || separador.isEmpty() ? "." : separador;
    }

    /** Sin secreto: el record nunca debe filtrarlo por logs. */
    @Override
    public String toString() {
        return "CatalogoParametrosProviderProperties[baseUrl=" + baseUrl + ", tokenId=" + tokenId
                + ", timeout=" + timeout + ", cacheTtl=" + cacheTtl + ", separador=" + separador + "]";
    }
}
