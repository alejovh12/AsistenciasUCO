package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.ActualizarSemestreEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ActualizarSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ResultadoSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoordinadorEstudianteSemestreControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final String RUTA = "/api/v1/coordinador/estudiantes/" + ESTUDIANTE + "/programas/" + PROGRAMA + "/semestre";

    private final ActualizarSemestreEstudianteInputPort port = mock(ActualizarSemestreEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CoordinadorEstudianteSemestreController(port, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void responde_200_con_el_mensaje_de_la_db_y_el_semestre() throws Exception {
        when(port.execute(new ActualizarSemestreEstudianteDTO(ACTOR, ESTUDIANTE, PROGRAMA, 3)))
                .thenReturn(new ResultadoSemestreEstudianteDTO("El semestre del estudiante quedó actualizado."));

        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"semestreActual\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.semestreActual").value(3))
                .andExpect(jsonPath("$.mensajeUsuario").value("El semestre del estudiante quedó actualizado."));
    }

    @Test
    void semestre_nulo_limpia() throws Exception {
        when(port.execute(any())).thenReturn(new ResultadoSemestreEstudianteDTO("ok"));

        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"semestreActual\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeUsuario").value("ok"));
        verify(port).execute(new ActualizarSemestreEstudianteDTO(ACTOR, ESTUDIANTE, PROGRAMA, null));
    }

    @Test
    void rechazo_de_la_db_se_traduce_a_403() throws Exception {
        doThrow(new ForbiddenException(SecurityErrorCode.FORBIDDEN)).when(port).execute(any());

        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"semestreActual\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void constructor_rechaza_nulos() {
        assertThrows(NullPointerException.class, () -> new CoordinadorEstudianteSemestreController(null, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorEstudianteSemestreController(port, null));
    }
}
