package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizacionHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizarHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.interactor.PrevisualizarHorarioGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.domain.PrevisualizarHorarioGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.impl.PrevisualizarHorarioGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioGrupoPrevisualizacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Resumen;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** SES-GEN-01: caso de uso e interactor de la previsualizacion del horario de un grupo (solo lectura). */
class PrevisualizarHorarioGrupoTest {

    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID EJECUTOR = UUID.randomUUID();
    private static final List<FranjaHorarioDomain> FRANJAS = List.of(
            new FranjaHorarioDomain("LU", null, LocalTime.of(8, 0), LocalTime.of(10, 0)));

    private final HorarioGrupoPrevisualizacionPort puerto = mock(HorarioGrupoPrevisualizacionPort.class);
    private final AsegurarFestivosPeriodoInputPort festivos = mock(AsegurarFestivosPeriodoInputPort.class);
    private final PrevisualizarHorarioGrupoUseCaseImpl useCase = new PrevisualizarHorarioGrupoUseCaseImpl(puerto, festivos);

    private static Resumen resumen(final Integer horasMateria, final int minutosSemanales, final int minutosProgramados,
                                   final Integer minutosEsperados, final Integer minutosConservados, final boolean existente) {
        return new Resumen(existente ? GRUPO : null, ASIGNATURA, PERIODO, "ARQ-402", "Arquitectura", "2027-1", "PLANEACION", "America/Bogota",
                LocalDate.of(2027, 2, 3), LocalDate.of(2027, 5, 25), horasMateria, minutosSemanales, 2, 112, 32, 30, 2, 0, 3840, minutosProgramados,
                240, 0, minutosEsperados, existente ? 3 : null, minutosConservados, existente ? 1 : null, existente ? 2 : null, existente ? 5 : null, true,
                existente);
    }

    private static PrevisualizacionHorarioGrupoProjection proyeccion(final Resumen resumen) {
        return new PrevisualizacionHorarioGrupoProjection(resumen, List.of(), List.of(), List.of(),
                List.of(new PrevisualizacionHorarioGrupoProjection.Conflicto("HOR_006", "BLOQUEANTE", "mensaje", null)));
    }

    @Test
    void un_horario_sin_franjas_se_rechaza_sin_consultar_la_db() {
        for (final List<FranjaHorarioDomain> franjas : java.util.Arrays.asList(null, List.<FranjaHorarioDomain>of())) {
            final var domain = new PrevisualizarHorarioGrupoDomain(null, ASIGNATURA, PERIODO, DOCENTE, franjas, EJECUTOR);
            final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(domain));
            assertEquals(MensajeUsuarioErrorCode.VAL_APP_076.code(), error.getCode());
        }
        verifyNoInteractions(puerto);
        verifyNoInteractions(festivos);
    }

    @Test
    void un_grupo_nuevo_exige_asignatura_periodo_y_docente() {
        assertThrows(ValidationException.class, () -> useCase.execute(new PrevisualizarHorarioGrupoDomain(null, null, PERIODO, DOCENTE, FRANJAS, EJECUTOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(new PrevisualizarHorarioGrupoDomain(null, ASIGNATURA, null, DOCENTE, FRANJAS, EJECUTOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(new PrevisualizarHorarioGrupoDomain(null, ASIGNATURA, PERIODO, null, FRANJAS, EJECUTOR)));
        verifyNoInteractions(puerto);
    }

    @Test
    void un_grupo_nuevo_asegura_los_festivos_del_periodo_antes_de_calcular() {
        final var esperado = proyeccion(resumen(4, 240, 3600, 3840, null, false));
        when(puerto.previsualizar(eq(null), eq(ASIGNATURA), eq(PERIODO), eq(DOCENTE), any(), eq(EJECUTOR))).thenReturn(esperado);

        final var resultado = useCase.execute(new PrevisualizarHorarioGrupoDomain(null, ASIGNATURA, PERIODO, DOCENTE, FRANJAS, EJECUTOR));

        assertEquals(esperado, resultado);
        final var orden = inOrder(festivos, puerto);
        orden.verify(festivos).execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(PERIODO));
        orden.verify(puerto).previsualizar(eq(null), eq(ASIGNATURA), eq(PERIODO), eq(DOCENTE), any(), eq(EJECUTOR));
    }

    @Test
    void un_grupo_existente_asegura_los_festivos_del_grupo_y_no_necesita_materia_ni_periodo() {
        when(puerto.previsualizar(eq(GRUPO), eq(null), eq(null), eq(null), any(), eq(EJECUTOR))).thenReturn(proyeccion(resumen(4, 240, 3600, 3840, 480, true)));

        useCase.execute(new PrevisualizarHorarioGrupoDomain(GRUPO, null, null, null, FRANJAS, EJECUTOR));

        verify(festivos).execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(GRUPO));
    }

    @Test
    void el_interactor_traduce_la_materia_la_diferencia_semanal_y_el_total_esperado() {
        final var interactor = new PrevisualizarHorarioGrupoInteractor(useCase);
        // 4 h de la materia = 240 min; el horario suma 180 min (3 h): no coincide, diferencia -60 min
        when(puerto.previsualizar(any(), any(), any(), any(), any(), any())).thenReturn(proyeccion(resumen(4, 180, 3600, 3840, null, false)));

        final PrevisualizacionHorarioGrupoDTO dto = interactor.execute(new PrevisualizarHorarioGrupoDTO(
                null, ASIGNATURA, PERIODO, DOCENTE, List.of(new FranjaHorarioDTO("LU", "08:00", "10:00")), EJECUTOR));

        assertEquals(4, dto.asignatura().horasSemanales());
        assertEquals(16, dto.periodo().semanasPeriodo());
        assertEquals(240, dto.resumenSemanal().minutosSemanalesMateria());
        assertEquals(-60, dto.resumenSemanal().diferenciaMinutos());
        assertFalse(dto.resumenSemanal().coincideConMateria());
        assertEquals(3840, dto.resumenPeriodo().minutosEsperadosMateria());
        assertEquals(-240, dto.resumenPeriodo().diferenciaMinutos());
        assertFalse(dto.resumenPeriodo().cubreTotalEsperado());
        assertNull(dto.cambioSobreGrupoExistente());
        assertEquals("HOR_006", dto.conflictos().getFirst().codigo());
        assertTrue(dto.puedeConfirmar());
    }

    @Test
    void sin_horas_de_la_materia_los_campos_de_comparacion_son_nulos_y_un_grupo_existente_trae_su_cambio() {
        final var interactor = new PrevisualizarHorarioGrupoInteractor(useCase);
        when(puerto.previsualizar(any(), any(), any(), any(), any(), any())).thenReturn(proyeccion(resumen(null, 240, 3600, null, 480, true)));

        final PrevisualizacionHorarioGrupoDTO dto = interactor.execute(new PrevisualizarHorarioGrupoDTO(
                GRUPO, null, null, null, List.of(new FranjaHorarioDTO("LU", "08:00", "10:00")), EJECUTOR));

        assertNull(dto.asignatura().horasSemanales());
        assertNull(dto.resumenSemanal().minutosSemanalesMateria());
        assertNull(dto.resumenSemanal().coincideConMateria());
        assertNull(dto.resumenPeriodo().minutosEsperadosMateria());
        assertNull(dto.resumenPeriodo().cubreTotalEsperado());
        assertNotNull(dto.cambioSobreGrupoExistente());
        assertEquals(3, dto.cambioSobreGrupoExistente().sesionesConservadas());
        assertEquals(480, dto.cambioSobreGrupoExistente().minutosConservados());
        assertEquals(2, dto.cambioSobreGrupoExistente().sesionesFuturasQueSeCrean());
        assertEquals(GRUPO, dto.idGrupo());
    }

    @Test
    void un_grupo_existente_suma_lo_conservado_al_total_programado_para_decidir_si_cubre_lo_esperado() {
        final var interactor = new PrevisualizarHorarioGrupoInteractor(useCase);
        // programado 3600 + conservado 240 = 3840 = esperado: cubre
        when(puerto.previsualizar(any(), any(), any(), any(), any(), any())).thenReturn(proyeccion(resumen(4, 240, 3600, 3840, 240, true)));

        final PrevisualizacionHorarioGrupoDTO dto = interactor.execute(new PrevisualizarHorarioGrupoDTO(
                GRUPO, null, null, null, List.of(new FranjaHorarioDTO("LU", "08:00", "10:00")), EJECUTOR));

        assertEquals(0, dto.resumenPeriodo().diferenciaMinutos());
        assertTrue(dto.resumenPeriodo().cubreTotalEsperado());
    }
}
