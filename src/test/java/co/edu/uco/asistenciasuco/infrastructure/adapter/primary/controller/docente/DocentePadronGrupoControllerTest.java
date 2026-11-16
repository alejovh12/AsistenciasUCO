package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.ConsultarPadronGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CIERRE-W1A: {@code GET /api/v1/docente/grupos/{grupoId}/estudiantes} (padron paginado, ApiPageResponse, 400 por parametros invalidos). */
class DocentePadronGrupoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final ConsultarPadronGrupoInputPort padron = mock(ConsultarPadronGrupoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocentePadronGrupoController(padron, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void devuelve_la_pagina_con_los_metadatos_y_delega_con_el_usuario_autenticado() throws Exception {
        final UUID matricula = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(padron.execute(new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 2, 12, "ana")))
                .thenReturn(new PadronGrupoPaginaDTO(List.of(
                        new EstudianteGrupoDTO(matricula, estudiante, "100", "Ana Perez", "a@x.edu", "A", "Activo")), 2, 12, 25L));

        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO)
                        .param("pagina", "2").param("tamano", "12").param("busqueda", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(12))
                .andExpect(jsonPath("$.totalElementos").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.elementos[0].id").value(matricula.toString()))
                .andExpect(jsonPath("$.elementos[0].idEstudiante").value(estudiante.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.elementos[0].codigoEstado").value("A"));
    }

    @Test
    void sin_parametros_usa_la_pagina_y_el_tamano_por_defecto_y_sin_busqueda() throws Exception {
        when(padron.execute(new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 1, 10, null)))
                .thenReturn(new PadronGrupoPaginaDTO(List.of(), 1, 10, 0L));

        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void pagina_o_tamano_fuera_de_rango_es_400_pag_001_y_no_llega_al_caso_de_uso() throws Exception {
        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO).param("pagina", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO).param("tamano", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO).param("tamano", "0"))
                .andExpect(status().isBadRequest());

        verify(padron, never()).execute(any());
    }

    @Test
    void busqueda_de_mas_de_100_caracteres_es_400_y_no_llega_al_caso_de_uso() throws Exception {
        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO).param("busqueda", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("busqueda"));

        verify(padron, never()).execute(any());
    }

    @Test
    void busqueda_de_exactamente_100_caracteres_es_valida() throws Exception {
        final String cien = "x".repeat(100);
        when(padron.execute(new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 1, 10, cien)))
                .thenReturn(new PadronGrupoPaginaDTO(List.of(), 1, 10, 0L));

        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", GRUPO).param("busqueda", cien))
                .andExpect(status().isOk());
    }

    @Test
    void un_grupo_que_no_es_uuid_es_400() throws Exception {
        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/estudiantes", "no-es-uuid"))
                .andExpect(status().isBadRequest());

        verify(padron, never()).execute(any());
    }
}
