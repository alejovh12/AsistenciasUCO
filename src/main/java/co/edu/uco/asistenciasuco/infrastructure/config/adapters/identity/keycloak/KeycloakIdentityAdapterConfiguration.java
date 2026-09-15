package co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak.KeycloakIdentityProviderAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.KeycloakIdentityProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root: selecciona Keycloak como {@link IdentityProviderPort} para provisioning
 * administrativo (crear/verificar/eliminar cuentas institucionales, asignar client roles).
 *
 * <p>Único lugar que conecta {@code IdentityProviderPort} → {@code KeycloakIdentityProviderAdapter}.
 * El adapter NO se autoregistra con {@code @Component}/{@code @Service}/{@code @Repository}.</p>
 *
 * <p>Deliberadamente NO registra nada de runtime security (eso vive en
 * {@code KeycloakSecurityAdapterConfiguration}, bajo {@code app.adapters.security.provider}):
 * identity provisioning y runtime authentication son capabilities distintas, aunque hoy
 * compartan el mismo IdP.</p>
 *
 * <p>Para migrar a otro proveedor de identidad (p.ej. Auth0):</p>
 * <ol>
 *   <li>Implementar {@link IdentityProviderPort} en un nuevo adapter.</li>
 *   <li>Crear sus properties tipadas bajo {@code app.providers.<nuevo>}.</li>
 *   <li>Crear una configuración equivalente a esta, condicionada a
 *       {@code app.adapters.identity.provider=<nuevo>}.</li>
 *   <li>No se requiere ningún cambio en Domain, Application, UseCase ni Controller.</li>
 * </ol>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.identity",
        name = "provider",
        havingValue = "keycloak",
        matchIfMissing = true
)
@EnableConfigurationProperties(KeycloakIdentityProviderProperties.class)
public class KeycloakIdentityAdapterConfiguration {

    @Bean
    public IdentityProviderPort identityProviderPort(final KeycloakIdentityProviderProperties properties) {
        return new KeycloakIdentityProviderAdapter(
                properties.serverUrl(),
                properties.realm(),
                properties.adminClientId(),
                properties.adminClientSecret(),
                properties.apiClientId(),
                properties.userIdAttribute()
        );
    }
}
