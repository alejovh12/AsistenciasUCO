package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * INT-003: identidad de servicio con la que el backend lee MessageUcoLab 0.3.0 (rol tecnico {@code MESSAGE_CONSUMER} del
 * componente de Seguridad). Desde 0.3.0 ya no hay rutas publicas ni identificadores de aplicacion/ambiente: MessageUcoLab
 * deduce el ambiente de la identidad.
 *
 * <p>Solo se exige con {@code app.adapters.message-catalog.provider=messageucolab}. El secreto llega del baul
 * ({@code asistencias-messageucolab-client-secret}) o de {@code MESSAGEUCOLAB_CLIENT_SECRET} en local; nunca va en Git.</p>
 *
 * @param tokenUri       endpoint de token de Keycloak del componente de Seguridad (grant {@code client_credentials}).
 * @param clientId       Client ID de la identidad ({@code svc-...}).
 * @param clientSecret   secreto de la identidad.
 * @param requestTimeout espera maxima de la peticion de token.
 */
@ConfigurationProperties(prefix = "app.providers.messageucolab.service-identity")
public record MessageUcoLabServiceIdentityProperties(
        URI tokenUri,
        String clientId,
        String clientSecret,
        Duration requestTimeout
) {

    public MessageUcoLabServiceIdentityProperties {
        if (tokenUri == null) {
            throw new IllegalStateException("app.providers.messageucolab.service-identity.token-uri es obligatorio.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("app.providers.messageucolab.service-identity.client-id es obligatorio.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("app.providers.messageucolab.service-identity.client-secret es obligatorio.");
        }
        clientId = clientId.trim();
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalStateException("app.providers.messageucolab.service-identity.request-timeout debe ser positivo.");
        }
    }

    /** No expone el secreto en logs ni en reportes de configuracion. */
    @Override
    public String toString() {
        return "MessageUcoLabServiceIdentityProperties[tokenUri=" + tokenUri + ", clientId=" + clientId
                + ", clientSecret=***, requestTimeout=" + requestTimeout + "]";
    }
}
