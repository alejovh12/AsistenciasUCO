package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CierrePeriodoReporteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** F-ADM-006: historial de cierres masivos leido de la auditoria (mapeo de columnas, fechas heterogeneas y metadata JSON). */
class CierrePeriodoHistorialJpaRepositoryTest {

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CierrePeriodoJpaRepository repo;

    @BeforeEach
    void preparar() {
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        repo = new CierrePeriodoJpaRepository(new JpaProcedureExecutor(manager), manager);
    }

    @Test
    void sin_entity_manager_el_historial_es_vacio() {
        assertTrue(new CierrePeriodoJpaRepository(new JpaProcedureExecutor(manager)).consultarHistorialCierres().isEmpty());
        assertThrows(NullPointerException.class, () -> new CierrePeriodoJpaRepository(null, manager));
    }

    @Test
    void mapea_metadata_resultado_y_actor_de_cada_fila() {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime fecha = OffsetDateTime.parse("2026-11-08T10:00:00Z");
        when(query.getResultList()).thenReturn(filas(
                new Object[]{id.toString(), "2026-2", fecha, "{\"procesados\": 40, \"reprobadosPorFallas\":5,\"materiasAfectadas\":7}", "SUCCESS", "admin-1"}));

        final List<CierrePeriodoReporteProjection> historial = repo.consultarHistorialCierres();

        verify(manager).createNativeQuery(CierrePeriodoJpaRepository.SQL_CONSULTAR_HISTORIAL);
        assertEquals(1, historial.size());
        final CierrePeriodoReporteProjection fila = historial.getFirst();
        assertEquals(id, fila.id());
        assertEquals("2026-2", fila.periodoCodigo());
        assertEquals(fecha, fila.fechaEjecucion());
        assertEquals(40, fila.totalEstudiantesProcesados());
        assertEquals(7, fila.totalMateriasAfectadas());
        assertEquals(35, fila.totalAprobadosAsistencia());
        assertEquals(5, fila.totalReprobadosFallas());
        assertEquals("COMPLETADO", fila.estado());
        assertEquals("admin-1", fila.ejecutadoPor());
    }

    @Test
    void sin_metadata_ni_actor_ni_periodo_usa_los_valores_por_defecto_y_estado_con_alertas() {
        when(query.getResultList()).thenReturn(filas(
                new Object[]{UUID.randomUUID(), null, Instant.parse("2026-11-08T10:00:00Z"), null, "FAILURE", " "}));

        final CierrePeriodoReporteProjection fila = repo.consultarHistorialCierres().getFirst();

        assertEquals("Periodo", fila.periodoCodigo());
        assertEquals(0, fila.totalEstudiantesProcesados());
        assertEquals(0, fila.totalAprobadosAsistencia());
        assertEquals("CON_ALERTAS", fila.estado());
        assertEquals("ADMIN_SISTEMA", fila.ejecutadoPor());
        assertEquals(OffsetDateTime.parse("2026-11-08T10:00:00Z"), fila.fechaEjecucion());
    }

    @Test
    void acepta_fechas_local_date_time_date_texto_y_nula_y_descarta_filas_cortas() {
        final Object[] corta = {UUID.randomUUID(), "x"};
        when(query.getResultList()).thenReturn(filas(
                new Object[]{UUID.randomUUID(), "p1", LocalDateTime.of(2026, 11, 8, 10, 0), "{}", "SUCCESS", "a"},
                new Object[]{UUID.randomUUID(), "p2", Date.from(Instant.parse("2026-11-08T11:00:00Z")), "{\"procesados\":3}", "SUCCESS", "a"},
                new Object[]{UUID.randomUUID(), "p3", "2026-11-08 12:00:00+00:00", "{\"procesados\":3,\"reprobadosPorFallas\":9}", "SUCCESS", "a"},
                new Object[]{UUID.randomUUID(), "p4", "no es una fecha", "{}", "SUCCESS", "a"},
                new Object[]{UUID.randomUUID(), "p5", null, "{}", "SUCCESS", "a"},
                corta));

        final List<CierrePeriodoReporteProjection> historial = repo.consultarHistorialCierres();

        assertEquals(5, historial.size());
        assertEquals(OffsetDateTime.of(2026, 11, 8, 10, 0, 0, 0, ZoneOffset.UTC), historial.get(0).fechaEjecucion());
        assertEquals(OffsetDateTime.parse("2026-11-08T11:00:00Z"), historial.get(1).fechaEjecucion());
        assertEquals(OffsetDateTime.parse("2026-11-08T12:00:00Z"), historial.get(2).fechaEjecucion());
        assertEquals(0, historial.get(2).totalAprobadosAsistencia());
        assertNotNull(historial.get(3).fechaEjecucion());
        assertNotNull(historial.get(4).fechaEjecucion());
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
