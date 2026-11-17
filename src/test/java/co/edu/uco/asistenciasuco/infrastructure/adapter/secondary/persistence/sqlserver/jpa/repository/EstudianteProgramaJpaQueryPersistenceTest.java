package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteProgramaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-05 ACADEMIC: consulta JPA de estudiantes por programa sobre {@code uv_estudiante_programa}.
 * Paridad de shape y orden con el baseline JDBC: {@code EstudianteProgramaJdbcBaseline} (solo src/test).
 */
class EstudianteProgramaJpaRepositoryTest {

    private static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USUARIO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TypedQuery<EstudianteProgramaQueryRow> typedQuery = mock(TypedQuery.class);
    private EstudianteProgramaJpaRepository persistence;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        persistence = new EstudianteProgramaJpaRepository(entityManager);
        when(entityManager.createQuery(anyString(), eq(EstudianteProgramaQueryRow.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void mapea_la_fila_jpql_a_la_proyeccion_con_numero_identificacion_como_texto() {
        when(typedQuery.getResultList()).thenReturn(List.of(
                new EstudianteProgramaQueryRow(EP_ID, USUARIO, 12345, "Ana Perez", "ana@uco.edu.co", PROGRAMA, "Ingenieria")));

        final List<EstudianteProgramaProjection> resultado = persistence.consultarEstudiantesPorPrograma(PROGRAMA);

        assertEquals(List.of(new EstudianteProgramaProjection(
                EP_ID, USUARIO, "12345", "Ana Perez", "ana@uco.edu.co", PROGRAMA, "Ingenieria")), resultado);
    }

    @Test
    void numero_identificacion_y_correo_nulos_se_proyectan_como_nulos() {
        when(typedQuery.getResultList()).thenReturn(List.of(
                new EstudianteProgramaQueryRow(EP_ID, USUARIO, null, "Ana Perez", null, PROGRAMA, "Ingenieria")));

        final EstudianteProgramaProjection proyeccion = persistence.consultarEstudiantesPorPrograma(PROGRAMA).getFirst();

        assertNull(proyeccion.numeroIdentificacion());
        assertNull(proyeccion.correo());
    }

    @Test
    void una_sola_consulta_por_lectura_con_filtro_de_programa_y_orden_original() {
        when(typedQuery.getResultList()).thenReturn(List.of());

        persistence.consultarEstudiantesPorPrograma(PROGRAMA);

        verify(entityManager, times(1)).createQuery(
                EstudianteProgramaJpaRepository.JPQL_ESTUDIANTES_POR_PROGRAMA, EstudianteProgramaQueryRow.class);
        verify(typedQuery).setParameter("idPrograma", PROGRAMA);
        assertTrue(EstudianteProgramaJpaRepository.JPQL_ESTUDIANTES_POR_PROGRAMA.contains("from UvEstudianteProgramaEntity ep"));
        assertTrue(EstudianteProgramaJpaRepository.JPQL_ESTUDIANTES_POR_PROGRAMA.contains("order by ei.nombreCompleto, ep.id"));
    }

    @Test
    void fallo_de_persistencia_se_traduce_a_database_operation_exception_conservando_causa() {
        final PersistenceException causa = new PersistenceException("fallo de base de datos");
        when(entityManager.createQuery(anyString(), eq(EstudianteProgramaQueryRow.class))).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarEstudiantesPorPrograma(PROGRAMA));
        assertSame(causa, exception.getCause());
    }
}




