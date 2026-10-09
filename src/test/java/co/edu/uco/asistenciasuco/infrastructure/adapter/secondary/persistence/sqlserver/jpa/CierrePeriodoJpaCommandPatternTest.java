package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-03 — patron del command {@code usp_ejecutar_cierre_masivo_periodo} sobre
 * {@code EntityManager + createNativeQuery("EXEC ...")}.
 */
class CierrePeriodoJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CierrePeriodoJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new CierrePeriodoJpaRepository(new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Cierre ejecutado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void CIE_PAT_001_cierre_usa_native_query_exec_con_binding_nombrado() {
        command.ejecutarCierreMasivoPeriodo("2026-1", "ACTOR-1", EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_ejecutar_cierre_masivo_periodo"));
        verify(query).setParameter("codigoPeriodo", "2026-1");
        verify(query).setParameter("idActor", "ACTOR-1");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void CIE_PAT_002_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.ejecutarCierreMasivoPeriodo("2026-1", "ACTOR-1", EJECUTOR));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void CIE_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.ejecutarCierreMasivoPeriodo("2026-1", "ACTOR-1", EJECUTOR));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }
}




