package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.KeycloakIdentityProviderAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del proveedor de identidad institucional.
 *
 * <p>Para cambiar de Keycloak a otro IdP en el futuro:
 * <ol>
 *   <li>Crear un nuevo adaptador que implemente {@link IdentityProviderPort}.</li>
 *   <li>Modificar el bean {@code identityProviderPort} en esta clase para instanciar el nuevo adaptador.</li>
 *   <li>No se requiere ningún otro cambio en la aplicación.</li>
 * </ol>
 * </p>
 */
@Configuration
public class IdentityProviderBeansConfig {

    @Bean
    public IdentityProviderPort identityProviderPort(
            @Value("${keycloak.admin.server-url:http://localhost:8081}") final String serverUrl,
            @Value("${keycloak.admin.realm:asistencias-uco}") final String realm,
            @Value("${keycloak.admin.admin-username:admin}") final String adminUsername,
            @Value("${keycloak.admin.admin-password:admin}") final String adminPassword,
            @Value("${keycloak.admin.client-id:admin-cli}") final String clientId
    ) {
        return new KeycloakIdentityProviderAdapter(serverUrl, realm, adminUsername, adminPassword, clientId);
    }
}
