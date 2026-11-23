package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Como se conecta Asistencias al componente externo "Catalogo de Parametros" (solo aplica con
 * {@code app.adapters.parameter-catalog.provider=catalogoparametros}).
 *
 * @param baseUrl       URL base de la API (sin barra final), p. ej. {@code http://localhost:8081/catalogo-parametros/api/v1}.
 * @param tokenUri      endpoint de token de Keycloak (Security Baseline); grant {@code client_credentials} (INT-004: M2M).
 * @param clientId      Client ID de la identidad de servicio asociada en el Catalogo a la aplicacion y al ambiente; el Catalogo
 *                      resuelve aplicacion y ambiente desde el JWT.
 * @param clientSecret  secreto de la identidad. Llega del baul o del {@code .env} local; nunca se registra en logs ni en errores.
 * @param applicationId aplicacion del Catalogo (opcional): si se define, los eventos de otra aplicacion se ignoran.
 * @param environmentId ambiente del Catalogo (opcional): si se define, los eventos de otro ambiente se ignoran.
 * @param timeout       espera maxima por la llamada HTTP (10 s por defecto: la lectura fria del componente tarda varios segundos).
 * @param cacheTtl      vigencia de la cache en memoria; {@code 0} la desactiva.
 * @param separador     separador entre grupo y clave dentro de {@code parametro.nombre}.
 */
@ConfigurationProperties(prefix = "app.providers.catalogo-parametros")
public record CatalogoParametrosProviderProperties(
        String baseUrl,
        URI tokenUri,
        String clientId,
        String clientSecret,
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
        if (tokenUri == null) {
            throw new IllegalStateException("app.providers.catalogo-parametros.token-uri es obligatorio.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.client-id es obligatorio.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("app.providers.catalogo-parametros.client-secret es obligatorio.");
        }
        clientId = clientId.trim();
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
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
        return "CatalogoParametrosProviderProperties[baseUrl=" + baseUrl + ", tokenUri=" + tokenUri + ", clientId=" + clientId
                + ", clientSecret=***, timeout=" + timeout + ", cacheTtl=" + cacheTtl + ", separador=" + separador + "]";
    }
}
