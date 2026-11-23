package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.primaryports.RetirarRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort.RecesoDia;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DEC-081: contrato HTTP de los dias de receso academico del administrador. */
class AdminRecesoAcademicoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID RECESO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/admin/periodos-academicos/{idPeriodo}/recesos";
    private static final String BODY = "{\"fecha\":\"2026-12-03\",\"motivo\":\"Jornada institucional\"}";
    private static final String MENSAJE = "Se registró el receso académico del 03/12/2026; se cancelaron 4 clases.";

    private final CrearRecesoAcademicoInputPort crear = mock(CrearRecesoAcademicoInputPort.class);
    private final RetirarRecesoAcademicoInputPort retirar = mock(RetirarRecesoAcademicoInputPort.class);
    private final ConsultarRecesosPeriodoInputPort consultar = mock(ConsultarRecesosPeriodoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminRecesoAcademicoController(crear, retirar, consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void crear_responde_201_con_el_dia_creado_el_mensaje_de_la_db_y_pasa_ruta_cuerpo_y_actor() throws Exception {
        when(crear.execute(any())).thenReturn(new CrearRecesoAcademicoInputPort.Resultado(RECESO, LocalDate.of(2026, 12, 3),
                "Jornada institucional", 4, 1, MENSAJE, 7, 2));

        mvc.perform(post(RUTA, PERIODO).contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(RECESO.toString()))
                .andExpect(jsonPath("$.datos.fecha").value("2026-12-03"))
                .andExpect(jsonPath("$.datos.motivo").value("Jornada institucional"))
                .andExpect(jsonPath("$.datos.sesionesCanceladas").value(4))
                .andExpect(jsonPath("$.datos.sesionesConAsistencia").value(1))
                .andExpect(jsonPath("$.datos.avisosEncolados").doesNotExist())
                .andExpect(jsonPath("$.mensajeUsuario").value(MENSAJE));

        final ArgumentCaptor<CrearRecesoAcademicoInputPort.Comando> comando = ArgumentCaptor.forClass(CrearRecesoAcademicoInputPort.Comando.class);
        verify(crear).execute(comando.capture());
        assertEquals(PERIODO, comando.getValue().idPeriodo());
        assertEquals(LocalDate.of(2026, 12, 3), comando.getValue().fecha());
        assertEquals("Jornada institucional", comando.getValue().motivo());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());
    }

    @Test
    void crear_preserva_los_rechazos_de_la_db_con_su_http() throws Exception {
        when(crear.execute(any()))
                .thenThrow(new ValidationException(MensajeUsuarioErrorCode.REC_004))
                .thenThrow(new ConflictException(MensajeUsuarioErrorCode.REC_006));

        mvc.perform(post(RUTA, PERIODO).contentType("application/json").content(BODY)).andExpect(status().isBadRequest());
        mvc.perform(post(RUTA, PERIODO).contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }

    @Test
    void retirar_responde_200_con_el_mensaje_y_409_si_el_dia_ya_cancelo_clases() throws Exception {
        when(retirar.execute(any()))
                .thenReturn(new RetirarRecesoAcademicoInputPort.Resultado("Se retiró el receso académico del 03/12/2026."))
                .thenThrow(new ConflictException(MensajeUsuarioErrorCode.REC_010))
                .thenThrow(new ResourceNotFoundException(MensajeUsuarioErrorCode.REC_009));

        mvc.perform(delete(RUTA + "/{idReceso}", PERIODO, RECESO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Se retiró el receso académico del 03/12/2026."));
        mvc.perform(delete(RUTA + "/{idReceso}", PERIODO, RECESO)).andExpect(status().isConflict());
        mvc.perform(delete(RUTA + "/{idReceso}", PERIODO, RECESO)).andExpect(status().isNotFound());

        final ArgumentCaptor<RetirarRecesoAcademicoInputPort.Comando> comando = ArgumentCaptor.forClass(RetirarRecesoAcademicoInputPort.Comando.class);
        verify(retirar, org.mockito.Mockito.times(3)).execute(comando.capture());
        assertEquals(RECESO, comando.getValue().idReceso());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());
    }

    @Test
    void consultar_lista_los_dias_con_estado_aplicado_y_si_se_pueden_retirar() throws Exception {
        when(consultar.execute(any())).thenReturn(List.of(
                new RecesoDia(RECESO, LocalDate.of(2026, 12, 3), "Jornada", 4, true, false),
                new RecesoDia(UUID.randomUUID(), LocalDate.of(2026, 12, 4), "Otro", 0, false, true)));

        mvc.perform(get(RUTA, PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[0].id").value(RECESO.toString()))
                .andExpect(jsonPath("$.datos[0].fecha").value("2026-12-03"))
                .andExpect(jsonPath("$.datos[0].motivo").value("Jornada"))
                .andExpect(jsonPath("$.datos[0].sesionesCanceladas").value(4))
                .andExpect(jsonPath("$.datos[0].estaAplicado").value(true))
                .andExpect(jsonPath("$.datos[0].puedeRetirarse").value(false))
                .andExpect(jsonPath("$.datos[1].estaAplicado").value(false))
                .andExpect(jsonPath("$.datos[1].puedeRetirarse").value(true));

        final ArgumentCaptor<ConsultarRecesosPeriodoInputPort.Comando> comando = ArgumentCaptor.forClass(ConsultarRecesosPeriodoInputPort.Comando.class);
        verify(consultar).execute(comando.capture());
        assertEquals(PERIODO, comando.getValue().idPeriodo());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());
    }
}