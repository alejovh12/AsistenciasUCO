package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.PrevisualizarHorarioGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizacionHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizarHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SES-GEN-01: {@code POST /api/v1/coordinador/grupos/previsualizacion-horario} (solo lectura: 200, no 201). */
class PrevisualizacionHorarioGrupoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final String RUTA = "/api/v1/coordinador/grupos/previsualizacion-horario";

    private final PrevisualizarHorarioGrupoInputPort inputPort = mock(PrevisualizarHorarioGrupoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PrevisualizacionHorarioGrupoController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static PrevisualizacionHorarioGrupoDTO respuesta() {
        return new PrevisualizacionHorarioGrupoDTO(null,
                new PrevisualizacionHorarioGrupoDTO.Asignatura(ASIGNATURA, "ARQ-402", "Arquitectura", 4),
                new PrevisualizacionHorarioGrupoDTO.Periodo(PERIODO, "2027-1", LocalDate.of(2027, 2, 3), LocalDate.of(2027, 5, 25), 112, 16, "PLANEACION"),
                "America/Bogota",
                List.of(new PrevisualizacionHorarioGrupoDTO.Franja("LU", "Lunes", "08:00", "10:00", 120)),
                new PrevisualizacionHorarioGrupoDTO.ResumenSemanal(1, 120, 240, -120, false),
                new PrevisualizacionHorarioGrupoDTO.ResumenPeriodo(16, 16, 0, 0, 1920, 1920, 0, 0, 3840, -1920, false, 1, 120),
                List.of(),
                List.of(new PrevisualizacionHorarioGrupoDTO.Receso(LocalDate.of(2027, 3, 3), "Receso académico: Jornada", 1)),
                List.of(new PrevisualizacionHorarioGrupoDTO.Sesion(1, 1, LocalDate.of(2027, 2, 8), "08:00", "10:00",
                        Instant.parse("2027-02-08T13:00:00Z"), Instant.parse("2027-02-08T15:00:00Z"), 120, "PROGRAMADA", null, null),
                        new PrevisualizacionHorarioGrupoDTO.Sesion(2, 4, LocalDate.of(2027, 3, 3), "08:00", "10:00",
                        Instant.parse("2027-03-03T13:00:00Z"), Instant.parse("2027-03-03T15:00:00Z"), 120, "CANCELADA_POR_RECESO", null,
                        "Receso académico: Jornada")),
                null,
                List.of(new PrevisualizacionHorarioGrupoDTO.Conflicto("HOR_006", "BLOQUEANTE", "Las horas no coinciden.", null)),
                false);
    }

    private static String cuerpo(final String franjas) {
        return "{\"idAsignatura\":\"" + ASIGNATURA + "\",\"idPeriodoAcademico\":\"" + PERIODO + "\",\"idDocente\":\"" + DOCENTE + "\",\"franjas\":" + franjas + "}";
    }

    @Test
    void responde_200_con_el_calendario_y_los_conflictos_como_datos_y_pasa_el_usuario_autenticado() throws Exception {
        when(inputPort.execute(any())).thenReturn(respuesta());

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"dia\":\"Lunes\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.periodo.diasPeriodo").value(112))
                .andExpect(jsonPath("$.datos.periodo.semanasPeriodo").value(16))
                .andExpect(jsonPath("$.datos.resumenPeriodo.sesionesCalendario").value(16))
                .andExpect(jsonPath("$.datos.resumenPeriodo.sesionesEnReceso").value(1))
                .andExpect(jsonPath("$.datos.resumenPeriodo.minutosPerdidosReceso").value(120))
                .andExpect(jsonPath("$.datos.recesos[0].fecha").value("2027-03-03"))
                .andExpect(jsonPath("$.datos.recesos[0].motivo").value("Receso académico: Jornada"))
                .andExpect(jsonPath("$.datos.recesos[0].sesionesAfectadas").value(1))
                .andExpect(jsonPath("$.datos.sesiones[1].estado").value("CANCELADA_POR_RECESO"))
                .andExpect(jsonPath("$.datos.sesiones[1].motivoReceso").value("Receso académico: Jornada"))
                .andExpect(jsonPath("$.datos.resumenSemanal.coincideConMateria").value(false))
                .andExpect(jsonPath("$.datos.sesiones[0].fechaHoraInicioUtc").value("2027-02-08T13:00:00Z"))
                .andExpect(jsonPath("$.datos.conflictos[0].codigo").value("HOR_006"))
                .andExpect(jsonPath("$.datos.conflictos[0].severidad").value("BLOQUEANTE"))
                .andExpect(jsonPath("$.datos.puedeConfirmar").value(false));

        final ArgumentCaptor<PrevisualizarHorarioGrupoDTO> dto = ArgumentCaptor.forClass(PrevisualizarHorarioGrupoDTO.class);
        verify(inputPort).execute(dto.capture());
        assertEquals(ACTOR, dto.getValue().usuarioEjecutor());
        assertEquals(ASIGNATURA, dto.getValue().idAsignatura());
        assertEquals(PERIODO, dto.getValue().idPeriodoAcademico());
        assertEquals(DOCENTE, dto.getValue().idDocente());
        // el dia llega como nombre y se normaliza al codigo de dbo.Dia, igual que al crear el grupo
        assertEquals("LU", dto.getValue().franjas().getFirst().codigoDia());
        assertEquals("08:00", dto.getValue().franjas().getFirst().horaInicio());
    }

    @Test
    void un_horario_ausente_o_vacio_es_400_sin_llamar_al_caso_de_uso() throws Exception {
        for (final String franjas : List.of("[]", "null")) {
            mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(franjas)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        verify(inputPort, never()).execute(any());
    }

    @Test
    void una_hora_ilegible_es_400() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"dia\":\"LU\",\"horaInicio\":\"8 de la mañana\",\"horaFin\":\"10:00\"}]")))
                .andExpect(status().isBadRequest());
        verify(inputPort, never()).execute(any());
    }

    @Test
    void el_rechazo_de_ambito_de_la_db_es_403_y_el_de_asignatura_o_periodo_no_disponible_409() throws Exception {
        when(inputPort.execute(any())).thenThrow(new ForbiddenException("SEC_002", "Sin autorizacion."));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"dia\":\"LU\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}]")))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.doThrow(new ConflictException("PER_010", "El periodo no esta vigente.")).when(inputPort).execute(any());
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"dia\":\"LU\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}]")))
                .andExpect(status().isConflict());
    }
}
