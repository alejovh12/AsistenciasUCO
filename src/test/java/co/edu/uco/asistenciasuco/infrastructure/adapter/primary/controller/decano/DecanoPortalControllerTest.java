package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DecanoPortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final ConsultarCoordinadoresInputPort consultarCoordinadores = mock(ConsultarCoordinadoresInputPort.class);
    private final CrearCoordinadorInputPort crearCoordinador = mock(CrearCoordinadorInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final DecanoPortalController controller =
            new DecanoPortalController(consultarCoordinadores, crearCoordinador, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarCoordinadores_usa_actor_autenticado_y_serializa_lista() throws Exception {
        when(consultarCoordinadores.execute(ACTOR)).thenReturn(List.of(new CoordinadorDTO(
                UUID.randomUUID(), UUID.randomUUID(), "123456", "Ana Perez", PROGRAMA, "Ingenieria", true)));

        mvc.perform(get("/api/v1/decano/coordinadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreCompleto").value("Ana Perez"));

        verify(consultarCoordinadores).execute(ACTOR);
    }

    @Test
    void toggleCoordinador_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class, () -> controller.toggleCoordinador(PROGRAMA)).getCode());
    }

    @Test
    void crearCoordinador_mapea_cuerpo_de_la_peticion_al_dto() throws Exception {
        final String body = "{\"numeroIdentificacion\":\"123456789\",\"primerNombre\":\"Ana\",\"segundoNombre\":\"Maria\","
                + "\"primerApellido\":\"Perez\",\"segundoApellido\":\"Gomez\",\"correo\":\"ana@uco.edu.co\","
                + "\"idPrograma\":\"" + PROGRAMA + "\",\"password\":\"Clave123!\"}";

        mvc.perform(post("/api/v1/decano/coordinadores").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<CrearCoordinadorDTO> captor = ArgumentCaptor.forClass(CrearCoordinadorDTO.class);
        verify(crearCoordinador).execute(captor.capture());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals(PROGRAMA, captor.getValue().idPrograma());
        assertEquals(ACTOR, captor.getValue().usuario());
    }

    @Test
    void crearCoordinador_con_peticion_nula_mapea_campos_nulos() {
        controller.crearCoordinador(null);

        final ArgumentCaptor<CrearCoordinadorDTO> captor = ArgumentCaptor.forClass(CrearCoordinadorDTO.class);
        verify(crearCoordinador).execute(captor.capture());
        assertNull(captor.getValue().numeroIdentificacion());
        assertEquals(ACTOR, captor.getValue().usuario());
    }
}
