package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
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
    void consultarCoordinadores_paginado_devuelve_api_page_response() throws Exception {
        when(consultarCoordinadores.execute(ACTOR, 1, 10)).thenReturn(new PaginaResultado<>(
                List.of(new CoordinadorDTO(UUID.randomUUID(), UUID.randomUUID(), "123456", "Ana Perez", PROGRAMA, "Ingenieria", true)),
                1, 10, 1));

        mvc.perform(get("/api/v1/decano/coordinadores").param("pagina", "1").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.numeroPagina").value(1))
                .andExpect(jsonPath("$.tamanoPagina").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(consultarCoordinadores).execute(ACTOR, 1, 10);
    }

    @Test
    void consultarCoordinadores_pagina_invalida_responde_400() throws Exception {
        mvc.perform(get("/api/v1/decano/coordinadores").param("pagina", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("PAG_001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details[0].field").value("pagina"));
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
