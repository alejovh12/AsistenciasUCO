package co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity.baseline.BaselineIdentityAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity.keycloak.KeycloakIdentityAdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El aprovisionamiento sigue al selector: baseline nunca contacta el Keycloak legacy y viceversa. */
class IdentityAdapterSelectionTest {

    private static final String[] LEGACY_PROPERTIES = {
            "app.providers.keycloak-identity.server-url=http://127.0.0.1:8081",
            "app.providers.keycloak-identity.realm=asistencias-uco",
            "app.providers.keycloak-identity.admin-client-id=test-only-admin",
            "app.providers.keycloak-identity.admin-client-secret=test-only-not-used",
            "app.providers.keycloak-identity.api-client-id=asistencias-api",
            "app.providers.keycloak-identity.user-id-attribute=idUsuario"
    };
    private static final String[] BASELINE_PROPERTIES = {
            "app.providers.security-baseline-identity.server-url=http://127.0.0.1:19090",
            "app.providers.security-baseline-identity.realm=security-baseline",
            "app.providers.security-baseline-identity.admin-client-id=asistencias-provisioner",
            "app.providers.security-baseline-identity.admin-client-secret=test-only-not-used",
            "app.providers.security-baseline-identity.api-client-id=asistencias-api",
            "app.providers.security-baseline-identity.user-id-attribute=idUsuario"
    };

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(KeycloakIdentityAdapterConfiguration.class, BaselineIdentityAdapterConfiguration.class)
                .withPropertyValues(LEGACY_PROPERTIES)
                .withPropertyValues(BASELINE_PROPERTIES);
    }

    @Test
    void baseline_registra_un_unico_identity_provider_port() {
        runner().withPropertyValues("app.adapters.identity.provider=baseline").run(context -> {
            assertEquals(1, context.getBeansOfType(IdentityProviderPort.class).size());
            assertNotNull(context.getBean(IdentityProviderPort.class));
            assertTrue(context.containsBean("identityProviderPort"));
        });
    }

    @Test
    void legacy_y_su_alias_keycloak_registran_el_adaptador_legacy() {
        for (final String value : new String[] {"legacy", "keycloak"}) {
            runner().withPropertyValues("app.adapters.identity.provider=" + value)
                    .run(context -> assertEquals(1, context.getBeansOfType(IdentityProviderPort.class).size()));
        }
    }

    @Test
    void valor_ausente_equivale_a_legacy() {
        runner().run(context -> assertEquals(1, context.getBeansOfType(IdentityProviderPort.class).size()));
    }

    @Test
    void valor_desconocido_no_registra_ningun_adaptador() {
        runner().withPropertyValues("app.adapters.identity.provider=basline")
                .run(context -> assertTrue(context.getBeansOfType(IdentityProviderPort.class).isEmpty()));
    }
}
