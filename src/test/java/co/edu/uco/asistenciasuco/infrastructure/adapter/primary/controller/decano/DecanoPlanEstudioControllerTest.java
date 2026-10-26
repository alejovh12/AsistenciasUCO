package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU108 (planes): malla de un plan de la facultad del decano bajo /api/v1/decano/planes-estudio. */
class DecanoPlanEstudioControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();

    private final ConsultarAsignaturasPlanInputPort inputPort = mock(ConsultarAsignaturasPlanInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DecanoPlanEstudioController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new DecanoPlanEstudioController(null, identity));
        assertThrows(NullPointerException.class, () -> new DecanoPlanEstudioController(inputPort, null));
    }

    @Test
    void T_108_01_devuelve_la_malla_del_plan_con_el_actor_autenticado() throws Exception {
        when(inputPort.execute(ACTOR, PLAN)).thenReturn(List.of(new AsignaturaDTO(UUID.randomUUID(), "A01", "Algebra", 3,
                UUID.randomUUID(), "Matematicas", UUID.randomUUID(), "Basico", UUID.randomUUID(), PLAN,
                UUID.randomUUID(), "Ingenieria", "2", true, "Activa")));

        mvc.perform(get("/api/v1/decano/planes-estudio/{id}/asignaturas", PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].nombre").value("Algebra"));
        verify(inputPort).execute(ACTOR, PLAN);
    }

    @Test
    void T_108_02_plan_de_otra_facultad_es_403() throws Exception {
        when(inputPort.execute(ACTOR, PLAN))
                .thenThrow(new ForbiddenException("El plan de estudio solicitado no pertenece al ambito del usuario autenticado."));

        mvc.perform(get("/api/v1/decano/planes-estudio/{id}/asignaturas", PLAN)).andExpect(status().isForbidden());
    }

    @Test
    void T_108_03_id_invalido_es_400() throws Exception {
        mvc.perform(get("/api/v1/decano/planes-estudio/{id}/asignaturas", "no-es-uuid")).andExpect(status().isBadRequest());
    }
}
