package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.ConsultarGruposDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/v1/decano/grupos}: grupos de la facultad del decano autenticado. */
class DecanoGrupoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConsultarGruposDecanoInputPort inputPort = mock(ConsultarGruposDecanoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DecanoGrupoController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void lista_los_grupos_del_decano_usando_el_actor_autenticado() throws Exception {
        final GrupoDTO grupo = new GrupoDTO(UUID.randomUUID(), "G1", "Grupo 1", UUID.randomUUID(), "Algebra",
                UUID.randomUUID(), 35, 20, 15, true, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 6, 15));
        when(inputPort.execute(ACTOR)).thenReturn(List.of(grupo));

        mvc.perform(get("/api/v1/decano/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].codigo").value("G1"))
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Algebra"));

        verify(inputPort).execute(ACTOR);
    }

    @Test
    void sin_facultad_de_decano_responde_403() throws Exception {
        when(inputPort.execute(ACTOR)).thenThrow(new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        mvc.perform(get("/api/v1/decano/grupos")).andExpect(status().isForbidden());
    }
}
