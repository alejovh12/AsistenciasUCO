package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DEC-081: consulta de solo lectura de los dias de receso para el coordinador y el decano. */
class CoordinadorRecesoAcademicoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/coordinador/periodos-academicos/{idPeriodo}/recesos";

    private final ConsultarRecesosPeriodoInputPort consultar = mock(ConsultarRecesosPeriodoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CoordinadorRecesoAcademicoController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void lista_los_dias_del_periodo_con_el_actor_autenticado() throws Exception {
        final UUID id = UUID.randomUUID();
        when(consultar.execute(any())).thenReturn(List.of(
                new RecesoDia(id, LocalDate.of(2026, 12, 3), "Jornada", 2, true, false)));

        mvc.perform(get(RUTA, PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].id").value(id.toString()))
                .andExpect(jsonPath("$.datos[0].fecha").value("2026-12-03"))
                .andExpect(jsonPath("$.datos[0].sesionesCanceladas").value(2))
                .andExpect(jsonPath("$.datos[0].estaAplicado").value(true))
                .andExpect(jsonPath("$.datos[0].puedeRetirarse").value(false));

        final ArgumentCaptor<ConsultarRecesosPeriodoInputPort.Comando> comando = ArgumentCaptor.forClass(ConsultarRecesosPeriodoInputPort.Comando.class);
        verify(consultar).execute(comando.capture());
        assertEquals(PERIODO, comando.getValue().idPeriodo());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());
    }

    @Test
    void un_perfil_sin_ambito_responde_403() throws Exception {
        when(consultar.execute(any())).thenThrow(new ForbiddenException("SEC_001", "No tiene el perfil requerido para esta consulta."));

        mvc.perform(get(RUTA, PERIODO)).andExpect(status().isForbidden());
    }
}