package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SES-GEN-01: adaptador de {@code usp_previsualizar_horario_grupo}. El SP devuelve UN conjunto de resultados de 36 columnas fijas con filas
 * discriminadas por {@code tipoFila}; el adaptador lee por posicion y con conteo estricto, igual que las demas lecturas de SP.
 */
class HorarioGrupoPrevisualizacionJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID EJECUTOR = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private HorarioGrupoPrevisualizacionJpaRepository repo;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new HorarioGrupoPrevisualizacionJpaRepository(manager);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final String tipo, final int orden) {
        final Object[] c = new Object[HorarioGrupoPrevisualizacionJpaRepository.COLUMNAS_PREVISUALIZACION];
        c[0] = tipo;
        c[1] = orden;
        return c;
    }

    private static Object[] resumen() {
        final Object[] c = fila("RESUMEN", 0);
        c[3] = ASIGNATURA.toString();
        c[4] = PERIODO.toString();
        c[5] = "ARQ-402";
        c[6] = "Arquitectura de Software";
        c[7] = "2027-1";
        c[8] = "PLANEACION";
        c[9] = "America/Bogota";
        c[10] = LocalDate.of(2027, 2, 3);
        c[11] = LocalDate.of(2027, 5, 25);
        c[14] = 4;          // horasSemanalesMateria
        c[15] = 240;        // minutosSemanales
        c[16] = 2;          // franjasPorSemana
        c[17] = 112;        // diasPeriodo
        c[18] = 32;         // sesionesCalendario
        c[19] = 30;         // sesionesProgramadas
        c[20] = 2;          // sesionesEnFestivo
        c[21] = 0;          // sesionesNoProgramables
        c[22] = 3840;       // minutosCalendario
        c[23] = 3600;       // minutosProgramados
        c[24] = 240;        // minutosPerdidosFestivo
        c[25] = 0;          // minutosNoProgramables
        c[26] = 3840;       // minutosEsperadosMateria
        c[32] = true;       // puedeConfirmar
        c[33] = 0;          // esGrupoExistente (BIT como entero)
        c[34] = 1;          // sesionesEnReceso (DEC-081, entero19)
        c[35] = 120;        // minutosPerdidosReceso (DEC-081, entero20)
        return c;
    }

    private void respuesta(final Object... filas) {
        when(query.getResultList()).thenReturn(new ArrayList<Object>(List.of(filas)));
    }

    private static List<FranjaHorarioRepositoryDTO> franjas() {
        return List.of(new FranjaHorarioRepositoryDTO("LU", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new FranjaHorarioRepositoryDTO("MI", LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }

    @Test
    void envia_los_parametros_nombrados_y_las_franjas_como_json_del_contrato_de_guardar_horarios() {
        respuesta((Object) resumen());

        repo.previsualizar(null, ASIGNATURA, PERIODO, DOCENTE, franjas(), EJECUTOR);

        verify(manager).createNativeQuery(HorarioGrupoPrevisualizacionJpaRepository.SQL_PREVISUALIZAR);
        verify(query).setParameter("idGrupo", null);
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        final ArgumentCaptor<Object> json = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(org.mockito.ArgumentMatchers.eq("horariosJSON"), json.capture());
        assertEquals("[{\"codigoDia\":\"LU\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"},"
                + "{\"codigoDia\":\"MI\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}]", json.getValue());
    }

    @Test
    void mapea_el_resumen_las_franjas_las_sesiones_los_festivos_y_los_conflictos_por_posicion() {
        final Object[] franja = fila("FRANJA", 1);
        franja[5] = "LU";
        franja[6] = "Lunes";
        franja[7] = "08:00";
        franja[8] = "10:00";
        franja[14] = 120;
        final Object[] sesion = fila("SESION", 1);
        sesion[5] = "PROGRAMADA";
        sesion[7] = "08:00";
        sesion[8] = "10:00";
        sesion[10] = LocalDate.of(2027, 2, 3);
        sesion[12] = Timestamp.valueOf(LocalDateTime.of(2027, 2, 3, 13, 0)); // hora de pared de la columna UTC
        sesion[13] = LocalDateTime.of(2027, 2, 3, 15, 0);
        sesion[14] = 1;
        sesion[15] = 1;
        sesion[16] = 120;
        final Object[] sesionReceso = fila("SESION", 2);
        sesionReceso[5] = "CANCELADA_POR_RECESO";
        sesionReceso[6] = "Receso académico: Jornada institucional";
        sesionReceso[7] = "08:00";
        sesionReceso[8] = "10:00";
        sesionReceso[10] = LocalDate.of(2027, 2, 10);
        sesionReceso[12] = Timestamp.valueOf(LocalDateTime.of(2027, 2, 10, 13, 0));
        sesionReceso[13] = LocalDateTime.of(2027, 2, 10, 15, 0);
        sesionReceso[14] = 2;
        sesionReceso[15] = 1;
        sesionReceso[16] = 120;
        final Object[] receso = fila("RECESO", 1);
        receso[5] = "Receso académico: Jornada institucional";
        receso[10] = LocalDate.of(2027, 2, 10);
        receso[14] = 1;
        final Object[] festivo = fila("FESTIVO", 1);
        festivo[5] = "Festivo";
        festivo[10] = LocalDate.of(2027, 3, 22);
        festivo[14] = 1;
        final Object[] conflicto = fila("CONFLICTO", 1);
        conflicto[5] = "HOR_011";
        conflicto[6] = "INFORMATIVO";
        conflicto[7] = "4 h caen en días festivos.";
        respuesta(resumen(), franja, sesion, sesionReceso, festivo, receso, conflicto);

        final PrevisualizacionHorarioGrupoProjection p = repo.previsualizar(null, ASIGNATURA, PERIODO, DOCENTE, franjas(), EJECUTOR);

        assertEquals(ASIGNATURA, p.resumen().idAsignatura());
        assertNull(p.resumen().idGrupo());
        assertEquals(4, p.resumen().horasSemanalesMateria());
        assertEquals(32, p.resumen().sesionesCalendario());
        assertEquals(3840, p.resumen().minutosEsperadosMateria());
        assertNull(p.resumen().sesionesConservadas());
        assertTrue(p.resumen().puedeConfirmar());
        assertEquals(false, p.resumen().esGrupoExistente());
        assertEquals(LocalDate.of(2027, 5, 25), p.resumen().fechaFinPeriodo());
        assertEquals(List.of(new PrevisualizacionHorarioGrupoProjection.Franja("LU", "Lunes", "08:00", "10:00", 120)), p.franjas());
        assertEquals(2, p.sesiones().size());
        assertEquals(Instant.parse("2027-02-03T13:00:00Z"), p.sesiones().getFirst().fechaHoraInicioUtc());
        assertEquals(Instant.parse("2027-02-03T15:00:00Z"), p.sesiones().getFirst().fechaHoraFinUtc());
        assertEquals("PROGRAMADA", p.sesiones().getFirst().estado());
        assertNull(p.sesiones().getFirst().motivoReceso());
        // DEC-081: texto2 de una sesion en receso es el motivo con prefijo, no un nombre de festivo
        assertEquals("CANCELADA_POR_RECESO", p.sesiones().get(1).estado());
        assertEquals("Receso académico: Jornada institucional", p.sesiones().get(1).motivoReceso());
        assertNull(p.sesiones().get(1).nombreFestivo());
        assertEquals(1, p.resumen().sesionesEnReceso());
        assertEquals(120, p.resumen().minutosPerdidosReceso());
        assertEquals(List.of(new PrevisualizacionHorarioGrupoProjection.Receso(LocalDate.of(2027, 2, 10),
                "Receso académico: Jornada institucional", 1)), p.recesos());
        assertEquals(List.of(new PrevisualizacionHorarioGrupoProjection.Festivo(LocalDate.of(2027, 3, 22), "Festivo", 1)), p.festivos());
        assertEquals("HOR_011", p.conflictos().getFirst().codigo());
        assertEquals("INFORMATIVO", p.conflictos().getFirst().severidad());
    }

    @Test
    void una_fila_con_columnas_distintas_a_las_del_contrato_es_fallo_tecnico() {
        respuesta((Object) new Object[33]);
        final RuntimeException error = assertThrows(RuntimeException.class, () -> repo.previsualizar(null, ASIGNATURA, PERIODO, DOCENTE, franjas(), EJECUTOR));
        assertTrue(error.getCause().getMessage().contains("36"));
    }

    @Test
    void sin_fila_resumen_o_con_tipo_desconocido_es_fallo_tecnico() {
        respuesta((Object) fila("FRANJA", 1));
        assertThrows(RuntimeException.class, () -> repo.previsualizar(null, ASIGNATURA, PERIODO, DOCENTE, franjas(), EJECUTOR));

        respuesta((Object) fila("RARA", 1));
        assertThrows(RuntimeException.class, () -> repo.previsualizar(null, ASIGNATURA, PERIODO, DOCENTE, franjas(), EJECUTOR));
    }
}
