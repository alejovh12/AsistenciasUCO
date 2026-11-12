package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaTomaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MinutosSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaTomaEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MinutosSesionEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PC (puntos de control): lecturas de tomas y minutos por sesion del estudiante (aislamiento, troceo del IN y mapeo). */
class AsistenciaSesionEstudiantePuntosControlJpaRepositoryTest {

    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final AsistenciaSesionEstudianteJpaRepository repository = new AsistenciaSesionEstudianteJpaRepository(entityManager);

    @SuppressWarnings("unchecked")
    private <R> TypedQuery<R> consulta(final String hql, final Class<R> tipo, final List<R> filas) {
        final TypedQuery<R> query = mock(TypedQuery.class);
        when(entityManager.createQuery(hql, tipo)).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(filas);
        return query;
    }

    @Test
    void las_tomas_se_leen_aisladas_por_estudiante_y_asignatura_y_se_mapean() {
        final UUID toma = UUID.randomUUID();
        final UUID solicitud = UUID.randomUUID();
        final TypedQuery<AsistenciaTomaEstudianteQueryRow> query = consulta(
                AsistenciaSesionEstudianteJpaRepository.HQL_TOMAS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES,
                AsistenciaTomaEstudianteQueryRow.class,
                List.of(new AsistenciaTomaEstudianteQueryRow(SESION, toma, 2, 2, LocalDateTime.of(2026, 11, 10, 13, 40), "AUSENTE",
                        Boolean.TRUE, solicitud, "P", null),
                        new AsistenciaTomaEstudianteQueryRow(SESION, UUID.randomUUID(), 3, 3, null, "SIN_REGISTRO", null, null, null, null)));

        final List<AsistenciaTomaEstudianteProjection> tomas = repository.consultarTomasSesiones(ESTUDIANTE, ASIGNATURA, List.of(SESION));

        assertEquals(2, tomas.size());
        assertEquals(new AsistenciaTomaEstudianteProjection(SESION, toma, 2, 2, Instant.parse("2026-11-10T13:40:00Z"), "AUSENTE",
                true, solicitud, "P", null), tomas.getFirst());
        assertFalse(tomas.get(1).esReclamable());
        assertEquals(null, tomas.get(1).fechaPuntoControlUtc());
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idsSesion", List.of(SESION));
        assertTrue(AsistenciaSesionEstudianteJpaRepository.HQL_TOMAS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES.contains("eg.idEstudiante = :idEstudiante"));
        assertTrue(AsistenciaSesionEstudianteJpaRepository.HQL_TOMAS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES.contains("g.idAsignatura = :idAsignatura"));
    }

    @Test
    void los_minutos_se_leen_aislados_y_se_mapean() {
        consulta(AsistenciaSesionEstudianteJpaRepository.HQL_MINUTOS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES,
                MinutosSesionEstudianteQueryRow.class,
                List.of(new MinutosSesionEstudianteQueryRow(SESION, 120, 20, 0, 100, 0, "PARCIAL")));

        final List<MinutosSesionEstudianteProjection> minutos = repository.consultarMinutosSesiones(ESTUDIANTE, ASIGNATURA, List.of(SESION));

        assertEquals(List.of(new MinutosSesionEstudianteProjection(SESION, 120, 20, 0, 100, 0, "PARCIAL")), minutos);
        assertTrue(AsistenciaSesionEstudianteJpaRepository.HQL_MINUTOS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES.contains("eg.idEstudiante = :idEstudiante"));
    }

    @Test
    void sin_sesiones_no_se_consulta_nada() {
        assertTrue(repository.consultarTomasSesiones(ESTUDIANTE, ASIGNATURA, List.of()).isEmpty());
        assertTrue(repository.consultarMinutosSesiones(ESTUDIANTE, ASIGNATURA, null).isEmpty());
        verify(entityManager, never()).createQuery(any(String.class), any(Class.class));
    }

    @Test
    void el_in_se_trocea_en_bloques() {
        final List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < AsistenciaSesionEstudianteJpaRepository.TAMANO_BLOQUE_IN + 1; i++) {
            ids.add(UUID.randomUUID());
        }
        final TypedQuery<MinutosSesionEstudianteQueryRow> query = consulta(
                AsistenciaSesionEstudianteJpaRepository.HQL_MINUTOS_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES,
                MinutosSesionEstudianteQueryRow.class, List.of());

        repository.consultarMinutosSesiones(ESTUDIANTE, ASIGNATURA, ids);

        verify(query, times(2)).getResultList();
        verify(query).setParameter(eq("idsSesion"), eq(ids.subList(0, AsistenciaSesionEstudianteJpaRepository.TAMANO_BLOQUE_IN)));
    }

    @Test
    void un_fallo_de_persistencia_se_traduce_a_error_tecnico() {
        when(entityManager.createQuery(any(String.class), any(Class.class))).thenThrow(new PersistenceException("boom"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarTomasSesiones(ESTUDIANTE, ASIGNATURA, List.of(SESION)));
    }
}
