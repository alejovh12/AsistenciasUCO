package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.compatibility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalIdentitySchemaCompatibilityVerifierTest {

    private ExternalIdentitySchemaCompatibilityVerifier verifierReturning(final Object result) {
        final EntityManager manager = mock(EntityManager.class);
        final Query query = mock(Query.class);
        when(manager.createNativeQuery(ExternalIdentitySchemaCompatibilityVerifier.SQL_CONTRATO_IDENTIDAD_EXTERNA))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(result);
        return new ExternalIdentitySchemaCompatibilityVerifier(manager);
    }

    @Test
    void con_el_vinculo_desplegado_el_arranque_continua() {
        assertDoesNotThrow(() -> verifierReturning(1).run(null));
    }

    @Test
    void sin_el_vinculo_en_la_db_el_arranque_falla_sin_degradar_a_correo() {
        final var verifierReturning3 = verifierReturning(0);
        final IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> verifierReturning3.run(null));
        final var verifierReturning2 = verifierReturning("no-numerico");
        assertThrows(IllegalStateException.class, () -> verifierReturning2.run(null));
        org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("SEC-BASELINE-017"));
    }
}
