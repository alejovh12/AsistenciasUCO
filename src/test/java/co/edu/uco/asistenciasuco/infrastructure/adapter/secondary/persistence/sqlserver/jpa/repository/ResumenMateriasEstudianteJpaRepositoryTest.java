package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W2B: el resumen sale de {@code usp_consultar_resumen_materias_estudiante}; el adaptador solo lo ejecuta y lo mapea. */
class ResumenMateriasEstudianteJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void llama_al_sp_con_el_usuario_ejecutor_y_la_correlacion_y_mapea_las_cuatro_columnas() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{3, 2, new BigDecimal("4.5"), 9}));

        final var fila = new ResumenMateriasEstudianteJpaRepository(manager).consultarResumen(ACTOR);

        verify(manager).createNativeQuery(ResumenMateriasEstudianteJpaRepository.SQL_RESUMEN);
        assertTrue(ResumenMateriasEstudianteJpaRepository.SQL_RESUMEN.contains("usp_consultar_resumen_materias_estudiante"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        assertEquals(3, fila.totalMaterias());
        assertEquals(2, fila.materiasEnRiesgo());
        assertEquals(new BigDecimal("4.50"), fila.totalHorasFalta());
        assertEquals(9, fila.totalCreditos());
    }

    @Test
    void el_riesgo_nulo_de_la_base_se_conserva() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{2, null, BigDecimal.ZERO, 6L}));

        final var fila = new ResumenMateriasEstudianteJpaRepository(manager).consultarResumen(ACTOR);

        assertNull(fila.materiasEnRiesgo());
        assertEquals(6, fila.totalCreditos());
    }

    @Test
    void sin_usuario_ejecutor_no_llega_a_la_db() {
        final var repository = new ResumenMateriasEstudianteJpaRepository(manager);

        assertThrows(CrosscuttingException.class, () -> repository.consultarResumen(null));
        verify(manager, never()).createNativeQuery(anyString());
    }

    @Test
    void columnas_distintas_de_4_o_sin_fila_son_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[3]));
        assertInstanceOf(IllegalStateException.class, assertThrows(DatabaseOperationException.class,
                () -> new ResumenMateriasEstudianteJpaRepository(manager).consultarResumen(ACTOR)).getCause());

        when(query.getResultList()).thenReturn(List.of());
        assertInstanceOf(IllegalStateException.class, assertThrows(DatabaseOperationException.class,
                () -> new ResumenMateriasEstudianteJpaRepository(manager).consultarResumen(ACTOR)).getCause());
    }

    @Test
    void los_rechazos_del_sp_se_traducen_con_su_http() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        final var resumenMateriasEstudianteJpaRepository2 = new ResumenMateriasEstudianteJpaRepository(manager);
        assertThrows(ForbiddenException.class, () -> resumenMateriasEstudianteJpaRepository2.consultarResumen(ACTOR));

        doThrow(new PersistenceException("db down")).when(query).getResultList();
        final var resumenMateriasEstudianteJpaRepository = new ResumenMateriasEstudianteJpaRepository(manager);
        assertThrows(DatabaseOperationException.class, () -> resumenMateriasEstudianteJpaRepository.consultarResumen(ACTOR));
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ResumenMateriasEstudianteJpaRepository(null));
    }
}
