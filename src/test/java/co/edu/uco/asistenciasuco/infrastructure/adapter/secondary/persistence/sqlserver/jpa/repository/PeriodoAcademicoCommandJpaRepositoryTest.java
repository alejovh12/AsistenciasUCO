package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU096/HU097/HU098: EXEC de los SP publicos de periodos academicos con binding nombrado y correlacion. */
class PeriodoAcademicoCommandJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERIODO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDate INICIO = LocalDate.of(2027, 2, 1);
    private static final LocalDate FIN = LocalDate.of(2027, 6, 30);

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private PeriodoAcademicoCommandJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new PeriodoAcademicoCommandJpaRepository(new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_ejecuta_el_sp_con_nombre_fechas_y_ejecutor() {
        repository.crearPeriodoAcademico(PERIODO, "2027-1", INICIO, FIN, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_periodo_academico"));
        verify(query).setParameter("idPeriodo", PERIODO);
        verify(query).setParameter("nombre", "2027-1");
        verify(query).setParameter("fechaInicio", INICIO);
        verify(query).setParameter("fechaFin", FIN);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void actualizar_ejecuta_su_sp() {
        repository.actualizarPeriodoAcademico(PERIODO, "2027-2", INICIO, FIN, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_periodo_academico"));
        verify(query).setParameter("nombre", "2027-2");
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void eliminar_ejecuta_su_sp() {
        repository.eliminarPeriodoAcademico(PERIODO, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_eliminar_periodo_academico"));
        verify(query).setParameter("idPeriodo", PERIODO);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void comandos_exigen_ejecutor_sin_llegar_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.crearPeriodoAcademico(PERIODO, "x", INICIO, FIN, null));
        assertThrows(CrosscuttingException.class, () -> repository.actualizarPeriodoAcademico(PERIODO, "x", INICIO, FIN, null));
        assertThrows(CrosscuttingException.class, () -> repository.eliminarPeriodoAcademico(PERIODO, null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }
}
