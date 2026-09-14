package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdapterPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AdapterPropertiesConfiguration.class)
            .withPropertyValues(
                    "app.adapters.persistence.provider=sqlserver",
                    "app.adapters.identity.provider=keycloak",
                    "app.adapters.security.provider=keycloak",
                    "app.adapters.storage.provider=local",
                    "app.adapters.realtime.provider=local-sse",
                    "app.adapters.audit.provider=logging"
            );

    @Test
    void bind_persistence_provider_sqlserver() {
        contextRunner
                .withPropertyValues("app.adapters.persistence.provider=sqlserver")
                .run(context -> assertEquals(
                        PersistenceAdapterProperties.Provider.SQLSERVER,
                        context.getBean(PersistenceAdapterProperties.class).provider()
                ));
    }

    @Test
    void bind_identity_provider_keycloak() {
        contextRunner
                .withPropertyValues("app.adapters.identity.provider=keycloak")
                .run(context -> assertEquals(
                        IdentityAdapterProperties.Provider.KEYCLOAK,
                        context.getBean(IdentityAdapterProperties.class).provider()
                ));
    }

    @Test
    void bind_storage_provider_local() {
        contextRunner
                .withPropertyValues("app.adapters.storage.provider=local")
                .run(context -> assertEquals(
                        StorageAdapterProperties.Provider.LOCAL,
                        context.getBean(StorageAdapterProperties.class).provider()
                ));
    }

    @Test
    void bind_realtime_provider_local_sse() {
        contextRunner
                .withPropertyValues("app.adapters.realtime.provider=local-sse")
                .run(context -> assertEquals(
                        RealtimeAdapterProperties.Provider.LOCAL_SSE,
                        context.getBean(RealtimeAdapterProperties.class).provider()
                ));
    }

    @Test
    void bind_audit_provider_logging() {
        contextRunner
                .withPropertyValues("app.adapters.audit.provider=logging")
                .run(context -> assertEquals(
                        AuditAdapterProperties.Provider.LOGGING,
                        context.getBean(AuditAdapterProperties.class).provider()
                ));
    }

    @Test
    void invalid_provider_value_falla_binding_sin_fallback() {
        contextRunner
                .withPropertyValues("app.adapters.persistence.provider=technology-that-does-not-exist")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
