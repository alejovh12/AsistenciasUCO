package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.compatibility;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Guard de arranque del modo {@code baseline} (SEC-BASELINE-017): comprueba, con el mismo principal SQL del
 * runtime, que la base expone el vínculo de identidad externa (vista y los dos SP públicos). Si falta algo el
 * arranque falla: nunca se autentica contra una base que no puede registrar ni leer el vínculo
 * {@code issuer + subject → Usuario.id}, ni se degrada a resolver por correo.
 */
@Component
@ConditionalOnProperty(prefix = "app.adapters.security", name = "provider", havingValue = "baseline")
public class ExternalIdentitySchemaCompatibilityVerifier implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalIdentitySchemaCompatibilityVerifier.class);

    static final String SQL_CONTRATO_IDENTIDAD_EXTERNA = """
            SELECT CASE
                     WHEN COL_LENGTH('dbo.uv_identidad_externa', 'estaVigenteIdentidad') IS NOT NULL
                      AND OBJECT_ID('dbo.usp_vincular_identidad_externa', 'P') IS NOT NULL
                      AND OBJECT_ID('dbo.usp_revocar_identidad_externa', 'P') IS NOT NULL
                     THEN 1 ELSE 0
                   END
            """;

    private final EntityManager entityManager;

    public ExternalIdentitySchemaCompatibilityVerifier(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public void run(final ApplicationArguments args) {
        verify();
    }

    void verify() {
        final Object result = entityManager.createNativeQuery(SQL_CONTRATO_IDENTIDAD_EXTERNA).getSingleResult();
        if (!(result instanceof Number number) || number.intValue() != 1) {
            throw new IllegalStateException(
                    "El modo baseline esta activo (app.adapters.security.provider=baseline) pero la base de datos no "
                            + "expone el vinculo de identidad externa (dbo.uv_identidad_externa, "
                            + "dbo.usp_vincular_identidad_externa, dbo.usp_revocar_identidad_externa). Despliegue primero "
                            + "la migracion SEC-BASELINE-017 de la DB o use app.adapters.security.provider=legacy.");
        }
        LOGGER.info("Vinculo de identidad externa (SEC-BASELINE-017) verificado en la base de datos.");
    }
}
