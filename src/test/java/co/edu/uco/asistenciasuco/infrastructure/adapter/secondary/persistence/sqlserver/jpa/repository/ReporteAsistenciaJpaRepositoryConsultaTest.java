package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.ReporteAsistenciaQueryRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regresion P3 D2: el reporte excluye las solicitudes de sesion rechazadas y marca las sesiones canceladas. */
class ReporteAsistenciaJpaRepositoryConsultaTest {

    @Test
    void el_hql_excluye_solicitudes_rechazadas_y_trae_si_la_sesion_esta_cancelada() {
        final String hql = ReporteAsistenciaJpaRepository.HQL_REPORTE_GRUPO;

        assertTrue(hql.contains("UvSesionEstadoEntity"), "debe consultar el estado de la sesion (uv_sesion_estado)");
        assertTrue(hql.contains("solicitudRechazada"), "debe filtrar las solicitudes de sesion rechazadas");
        assertTrue(hql.contains("estaCancelada"), "debe traer si la sesion fue cancelada");
    }

    @Test
    void la_proyeccion_lleva_la_marca_de_sesion_cancelada_a_la_fila_del_reporte() {
        final EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked") final TypedQuery<ReporteAsistenciaQueryRow> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        final LocalDateTime t = LocalDateTime.of(2026, 11, 23, 13, 0);
        when(query.getResultList()).thenReturn(List.of(
                new ReporteAsistenciaQueryRow(1, "G", 2, "S2", t, t.plusHours(2), 1950000003, "Persona", "p@x", null, null, true),
                new ReporteAsistenciaQueryRow(1, "G", 3, "S3", t, t.plusHours(2), 1950000003, "Persona", "p@x", Boolean.TRUE, null, false)));

        final List<ReporteAsistenciaRow> filas = new ReporteAsistenciaJpaRepository(entityManager)
                .consultarReporteAsistenciaGrupo(UUID.randomUUID());

        assertTrue(filas.get(0).sesionCancelada());
        assertFalse(filas.get(1).sesionCancelada());
        assertEquals("1950000003", filas.get(0).documentoEstudiante());
    }

    private static Class<ReporteAsistenciaQueryRow> eq() {
        return org.mockito.ArgumentMatchers.eq(ReporteAsistenciaQueryRow.class);
    }
}
