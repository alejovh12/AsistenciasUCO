package co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.profilesync.ProfileSyncingIdentityProvider;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak.KeycloakIdentityProviderAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineIdentityProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root: en modo baseline el {@link IdentityProviderPort} (crear cuenta, asignar/retirar rol, habilitar y
 * deshabilitar) opera sobre el realm de identidad central del Security Baseline, nunca sobre el Keycloak legacy.
 *
 * <p>La identidad central del baseline es un Keycloak: el adaptador de Admin REST se reutiliza con la configuración
 * propia de {@code app.providers.security-baseline-identity}. {@code IdentityProviderPort}, los casos de uso y el
 * Domain no cambian.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.identity", name = "provider", havingValue = "baseline")
@EnableConfigurationProperties(SecurityBaselineIdentityProviderProperties.class)
public class BaselineIdentityAdapterConfiguration {

    @Bean
    public IdentityProviderPort identityProviderPort(
            final SecurityBaselineIdentityProviderProperties properties,
            final AccessProfilePort accessProfilePort,
            final UsuarioSincronizacionIdentidadPort desfases
    ) {
        // Asignar un client role no concede acceso: el decorador asegura tambien el perfil equivalente en el PDP.
        return new ProfileSyncingIdentityProvider(new KeycloakIdentityProviderAdapter(
                properties.serverUrl(),
                properties.realm(),
                properties.adminClientId(),
                properties.adminClientSecret(),
                properties.apiClientId(),
                properties.userIdAttribute()
        ), accessProfilePort, desfases);
    }
}
