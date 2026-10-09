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
 * LB-008 JPA-03 — patron de los commands de asignatura ({@code usp_crear_asignatura},
 * {@code usp_actualizar_asignatura}, {@code usp_toggle_estado_asignatura}) sobre
 * {@code EntityManager + createNativeQuery("EXEC ...")}. Verifica SQL emitido, binding nombrado,
 * resultado canonico validado y traduccion de fallos; no certifica SQL Server (eso es el IT de paridad).
 */
class AsignaturaJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ASIGNATURA = UUID.fromString("10101010-1010-1010-1010-101010101010");
    private static final UUID PLAN = UUID.fromString("20202020-2020-2020-2020-202020202020");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AsignaturaJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Asignatura creada.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void ASG_PAT_001_crear_asignatura_usa_native_query_exec_con_binding_nombrado() {
        command.crearAsignatura(ASIGNATURA, "MAT-1", "Calculo", 4, PLAN, 1, "CIENCIAS", "BASICO", EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_asignatura"));
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("codigo", "MAT-1");
        verify(query).setParameter("nombre", "Calculo");
        verify(query).setParameter("creditos", 4);
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("semestreNumero", 1);
        verify(query).setParameter("nombreArea", "CIENCIAS");
        verify(query).setParameter("nombreComponente", "BASICO");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void ASG_PAT_002_actualizar_asignatura_usa_usp_actualizar_sin_usuario_ejecutor() {
        command.actualizarAsignatura(ASIGNATURA, "MAT-1", "Calculo", 4, PLAN, 1, "CIENCIAS", "BASICO");

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_asignatura"));
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query, never()).setParameter(org.mockito.ArgumentMatchers.eq("idUsuarioEjecutor"), any());
        verify(query).getResultList();
    }

    @Test
    void ASG_PAT_003_toggle_estado_usa_usp_toggle_con_correlacion() {
        command.toggleEstadoAsignatura(ASIGNATURA);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_toggle_estado_asignatura"));
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).getResultList();
    }

    @Test
    void ASG_PAT_004_fallo_tecnico_del_sp_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("connection reset"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.toggleEstadoAsignatura(ASIGNATURA));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void ASG_PAT_005_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.toggleEstadoAsignatura(ASIGNATURA));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }
}





