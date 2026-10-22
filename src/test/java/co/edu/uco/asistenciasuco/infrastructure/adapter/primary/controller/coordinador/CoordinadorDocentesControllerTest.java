package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.ActualizarDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto.ActualizarDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocentesProgramaPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.ConsultarCandidatosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.ConsultarDocentesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.ConsultarDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU062/HU063/HU089/HU091: endpoints reales /api/v1/coordinador/docentes (antes 501). */
class CoordinadorDocentesControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();

    private final ConsultarDocentesProgramaInputPort consultar = mock(ConsultarDocentesProgramaInputPort.class);
    private final ConsultarCandidatosDocenteInputPort candidatos = mock(ConsultarCandidatosDocenteInputPort.class);
    private final VincularDocenteProgramaInputPort vincular = mock(VincularDocenteProgramaInputPort.class);
    private final ActualizarDocenteProgramaInputPort actualizar = mock(ActualizarDocenteProgramaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CoordinadorDocentesController(consultar, candidatos, vincular, actualizar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_062_01_get_usa_el_actor_autenticado_aplica_valores_por_defecto_y_serializa_la_pagina() throws Exception {
        when(consultar.execute(eq(ACTOR), any())).thenReturn(new DocentesProgramaPaginaDTO(List.of(dto()), 12));

        mvc.perform(get("/api/v1/coordinador/docentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.datos[0].idDocente").value(DOCENTE.toString()))
                .andExpect(jsonPath("$.datos[0].codigoTipoVinculacion").value("CAT"))
                .andExpect(jsonPath("$.datos[0].estaActivo").value(true));

        final ArgumentCaptor<ConsultarDocentesProgramaDTO> filtro = ArgumentCaptor.forClass(ConsultarDocentesProgramaDTO.class);
        verify(consultar).execute(eq(ACTOR), filtro.capture());
        assertEquals(1, filtro.getValue().pagina());
        assertEquals(10, filtro.getValue().tamano());
        assertNull(filtro.getValue().busqueda());
    }

    @Test
    void T_063_01_get_traslada_busqueda_y_filtros() throws Exception {
        when(consultar.execute(eq(ACTOR), any())).thenReturn(new DocentesProgramaPaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/coordinador/docentes")
                        .param("pagina", "2").param("tamano", "5").param("busqueda", "ros")
                        .param("estaActivo", "true").param("idTipoVinculacion", TIPO.toString()))
                .andExpect(status().isOk());

        final ArgumentCaptor<ConsultarDocentesProgramaDTO> filtro = ArgumentCaptor.forClass(ConsultarDocentesProgramaDTO.class);
        verify(consultar).execute(eq(ACTOR), filtro.capture());
        assertEquals(2, filtro.getValue().pagina());
        assertEquals(5, filtro.getValue().tamano());
        assertEquals("ros", filtro.getValue().busqueda());
        assertEquals(Boolean.TRUE, filtro.getValue().estaActivo());
        assertEquals(TIPO, filtro.getValue().idTipoVinculacion());
    }

    @Test
    void get_rechaza_paginacion_y_busqueda_invalidas_con_400() throws Exception {
        mvc.perform(get("/api/v1/coordinador/docentes").param("pagina", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/coordinador/docentes").param("tamano", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/coordinador/docentes").param("busqueda", "x".repeat(51))).andExpect(status().isBadRequest());
        verifyNoInteractions(consultar);
    }

    @Test
    void get_sin_ambito_responde_403() throws Exception {
        when(consultar.execute(eq(ACTOR), any())).thenThrow(new ForbiddenException("sin programa"));

        mvc.perform(get("/api/v1/coordinador/docentes")).andExpect(status().isForbidden());
    }

    @Test
    void T_091_18_candidatos_serializa_datos_minimos_sin_correo() throws Exception {
        when(candidatos.execute(ACTOR, "ros")).thenReturn(List.of(
                new DocenteCandidatoDTO(DOCENTE, UUID.randomUUID(), "CC", 1017555444, "Maria Rostagno")));

        mvc.perform(get("/api/v1/coordinador/docentes/candidatos").param("busqueda", "ros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreCompleto").value("Maria Rostagno"))
                .andExpect(jsonPath("$.datos[0].correo").doesNotExist());
    }

    @Test
    void T_091_19_candidatos_exige_busqueda_de_al_menos_tres_caracteres() throws Exception {
        mvc.perform(get("/api/v1/coordinador/docentes/candidatos").param("busqueda", "ab")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/coordinador/docentes/candidatos")).andExpect(status().isBadRequest());
        verifyNoInteractions(candidatos);
    }

    @Test
    void T_091_01_post_modo_nuevo_responde_201_y_arma_el_dto_con_el_actor() throws Exception {
        when(vincular.execute(any())).thenReturn(dto());

        mvc.perform(post("/api/v1/coordinador/docentes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idTipoIdentificacion":"%s","numeroIdentificacion":"1017555444","primerNombre":"Jorge",
                         "primerApellido":"Henao","correo":"jorge.henao@uco.edu.co","password":"Clave1234!",
                         "idTipoVinculacion":"%s"}""".formatted(UUID.randomUUID(), TIPO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idDocente").value(DOCENTE.toString()));

        final ArgumentCaptor<VincularDocenteProgramaDTO> captor = ArgumentCaptor.forClass(VincularDocenteProgramaDTO.class);
        verify(vincular).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(1017555444, captor.getValue().numeroIdentificacion());
        assertEquals("Clave1234!", captor.getValue().password());
        assertEquals(TIPO, captor.getValue().idTipoVinculacion());
        assertNull(captor.getValue().idDocente());
    }

    @Test
    void T_091_02_post_modo_existente_responde_201() throws Exception {
        when(vincular.execute(any())).thenReturn(dto());

        mvc.perform(post("/api/v1/coordinador/docentes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idDocente\":\"%s\",\"idTipoVinculacion\":\"%s\"}".formatted(DOCENTE, TIPO)))
                .andExpect(status().isCreated());

        final ArgumentCaptor<VincularDocenteProgramaDTO> captor = ArgumentCaptor.forClass(VincularDocenteProgramaDTO.class);
        verify(vincular).execute(captor.capture());
        assertEquals(DOCENTE, captor.getValue().idDocente());
    }

    @Test
    void post_invalido_responde_400_sin_llamar_al_caso_de_uso() throws Exception {
        mvc.perform(post("/api/v1/coordinador/docentes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(vincular);
    }

    @Test
    void T_091_04_conflicto_de_negocio_se_traduce_a_409() throws Exception {
        when(vincular.execute(any())).thenThrow(new ConflictException("El docente ya está vinculado a este programa."));

        mvc.perform(post("/api/v1/coordinador/docentes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idDocente\":\"%s\",\"idTipoVinculacion\":\"%s\"}".formatted(DOCENTE, TIPO)))
                .andExpect(status().isConflict());
    }

    @Test
    void T_092_01_put_actualiza_con_el_actor_y_el_docente_de_la_ruta() throws Exception {
        when(actualizar.execute(any())).thenReturn(dto());

        mvc.perform(put("/api/v1/coordinador/docentes/{id}", DOCENTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTipoVinculacion\":\"%s\",\"primerNombre\":\"Nuevo\",\"segundoApellido\":\"\"}".formatted(TIPO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idDocente").value(DOCENTE.toString()));

        final ArgumentCaptor<ActualizarDocenteProgramaDTO> captor = ArgumentCaptor.forClass(ActualizarDocenteProgramaDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(DOCENTE, captor.getValue().idDocente());
        assertEquals(TIPO, captor.getValue().idTipoVinculacion());
        assertEquals("Nuevo", captor.getValue().primerNombre());
        assertEquals("", captor.getValue().segundoApellido());
        assertNull(captor.getValue().segundoNombre());
    }

    @Test
    void T_092_03_put_sin_campos_responde_400_sin_llamar_al_caso_de_uso() throws Exception {
        mvc.perform(put("/api/v1/coordinador/docentes/{id}", DOCENTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(actualizar);
    }

    @Test
    void T_092_04_put_de_docente_sin_vinculo_responde_404_del_backend() throws Exception {
        when(actualizar.execute(any())).thenThrow(new co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException("no vinculado"));

        mvc.perform(put("/api/v1/coordinador/docentes/{id}", DOCENTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primerNombre\":\"Nuevo\"}"))
                .andExpect(status().isNotFound());
    }

    private static DocenteProgramaDTO dto() {
        return new DocenteProgramaDTO(UUID.randomUUID(), DOCENTE, UUID.randomUUID(), "CC", 1017555444, "Jorge", null,
                "Henao", null, "Jorge Henao", "jorge.henao@uco.edu.co", TIPO, "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 0, true);
    }
}
