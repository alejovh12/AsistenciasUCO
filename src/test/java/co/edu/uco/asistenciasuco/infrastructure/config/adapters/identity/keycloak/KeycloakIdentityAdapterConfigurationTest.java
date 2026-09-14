package co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.KeycloakIdentityProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeycloakIdentityAdapterConfigurationTest {

    private static final String[] VALID_KEYCLOAK_PROPERTIES = {
            "app.adapters.identity.provider=keycloak",
            "app.providers.keycloak-identity.server-url=http://localhost:8081",
            "app.providers.keycloak-identity.realm=asistencias-uco",
            "app.providers.keycloak-identity.admin-client-id=asistencias-backend-admin",
            "app.providers.keycloak-identity.admin-client-secret=test-only-not-used",
            "app.providers.keycloak-identity.api-client-id=asistencias-api",
            "app.providers.keycloak-identity.user-id-attribute=idUsuario"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KeycloakIdentityAdapterConfiguration.class);

    @Test
    void provider_keycloak_registra_exactamente_un_identityProviderPort() {
        contextRunner
                .withPropertyValues(VALID_KEYCLOAK_PROPERTIES)
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(IdentityProviderPort.class).size());
                    assertInstanceOf(
                            KeycloakIdentityProviderAdapter.class,
                            context.getBean(IdentityProviderPort.class)
                    );
                });
    }

    @Test
    void provider_keycloak_con_admin_client_secret_faltante_falla_startup() {
        contextRunner
                .withPropertyValues(
                        "app.adapters.identity.provider=keycloak",
                        "app.providers.keycloak-identity.server-url=http://localhost:8081",
                        "app.providers.keycloak-identity.realm=asistencias-uco",
                        "app.providers.keycloak-identity.admin-client-id=asistencias-backend-admin",
                        "app.providers.keycloak-identity.api-client-id=asistencias-api",
                        "app.providers.keycloak-identity.user-id-attribute=idUsuario"
                )
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
