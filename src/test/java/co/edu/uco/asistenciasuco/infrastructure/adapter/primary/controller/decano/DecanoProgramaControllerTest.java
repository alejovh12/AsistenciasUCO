package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.ActualizarProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.CambiarEstadoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DecanoProgramaControllerTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID COORDINADOR = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID MODALIDAD = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID JORNADA = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final ProgramaAcademicoDTO PROGRAMA_DTO = new ProgramaAcademicoDTO(
            PROGRAMA, "Sistemas", TIPO, "pregrado", MODALIDAD, "Presencial", JORNADA, "Diurna",
            COORDINADOR, "Ana Perez", FACULTAD, "Ingenieria", true);

    private final CrearProgramaInputPort crear = mock(CrearProgramaInputPort.class);
    private final ActualizarProgramaInputPort actualizar = mock(ActualizarProgramaInputPort.class);
    private final CambiarEstadoProgramaInputPort cambiarEstado = mock(CambiarEstadoProgramaInputPort.class);
    private final ConsultarProgramasInputPort consultar = mock(ConsultarProgramasInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DecanoProgramaController(crear, actualizar, cambiarEstado, consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static final String BODY = "{\"nombre\":\"Sistemas\",\"idTipoPrograma\":\"" + TIPO + "\",\"idCoordinador\":\""
            + COORDINADOR + "\",\"idModalidad\":\"" + MODALIDAD + "\",\"idJornada\":\"" + JORNADA + "\"}";

    @Test
    void T_108_01_get_lista_paginada_con_total_y_filtros() throws Exception {
        when(consultar.execute(any())).thenReturn(new ProgramasPaginaDTO(List.of(PROGRAMA_DTO), 12));

        mvc.perform(get("/api/v1/decano/programas")
                        .param("pagina", "2").param("tamano", "5").param("nombre", "Sis")
                        .param("estaActivo", "true").param("idTipoPrograma", TIPO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.datos[0].id").value(PROGRAMA.toString()))
                .andExpect(jsonPath("$.datos[0].nombre").value("Sistemas"))
                .andExpect(jsonPath("$.datos[0].nombreModalidad").value("Presencial"))
                .andExpect(jsonPath("$.datos[0].nombreCoordinador").value("Ana Perez"))
                .andExpect(jsonPath("$.datos[0].estaActivo").value(true));

        final ArgumentCaptor<ConsultarProgramasDTO> captor = ArgumentCaptor.forClass(ConsultarProgramasDTO.class);
        verify(consultar).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(2, captor.getValue().pagina());
        assertEquals(5, captor.getValue().tamano());
        assertEquals("Sis", captor.getValue().nombre());
        assertEquals(Boolean.TRUE, captor.getValue().estaActivo());
        assertEquals(TIPO, captor.getValue().idTipoPrograma());
    }

    @Test
    void get_sin_parametros_delega_valores_nulos_para_que_el_caso_de_uso_aplique_los_defectos() throws Exception {
        when(consultar.execute(any())).thenReturn(new ProgramasPaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/decano/programas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        final ArgumentCaptor<ConsultarProgramasDTO> captor = ArgumentCaptor.forClass(ConsultarProgramasDTO.class);
        verify(consultar).execute(captor.capture());
        assertNull(captor.getValue().pagina());
        assertNull(captor.getValue().tamano());
    }

    @Test
    void T_SEC_01_get_sin_ambito_de_decano_responde_403() throws Exception {
        when(consultar.execute(any())).thenThrow(new ForbiddenException("sin ambito"));

        mvc.perform(get("/api/v1/decano/programas")).andExpect(status().isForbidden());
    }

    @Test
    void T_076_01_post_crea_y_responde_201_con_el_programa() throws Exception {
        when(crear.execute(any())).thenReturn(PROGRAMA_DTO);

        mvc.perform(post("/api/v1/decano/programas").contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(PROGRAMA.toString()))
                .andExpect(jsonPath("$.datos.idCoordinador").value(COORDINADOR.toString()));

        final ArgumentCaptor<CrearProgramaDTO> captor = ArgumentCaptor.forClass(CrearProgramaDTO.class);
        verify(crear).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals("Sistemas", captor.getValue().nombre());
        assertEquals(TIPO, captor.getValue().idTipoPrograma());
        assertEquals(COORDINADOR, captor.getValue().idCoordinador());
        assertEquals(MODALIDAD, captor.getValue().idModalidad());
        assertEquals(JORNADA, captor.getValue().idJornada());
    }

    @Test
    void T_076_12_post_con_cuerpo_invalido_responde_400_sin_llamar_al_caso_de_uso() throws Exception {
        mvc.perform(post("/api/v1/decano/programas").contentType("application/json").content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/decano/programas").contentType("application/json")
                        .content("{\"nombre\":\"Sistemas\",\"idTipoPrograma\":\"" + TIPO + "\"}"))
                .andExpect(status().isBadRequest());

        verify(crear, never()).execute(any());
    }

    @Test
    void T_076_03_post_con_nombre_duplicado_responde_409() throws Exception {
        when(crear.execute(any())).thenThrow(new ConflictException("duplicado"));

        mvc.perform(post("/api/v1/decano/programas").contentType("application/json").content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void T_077_01_put_actualiza_y_responde_200() throws Exception {
        when(actualizar.execute(any())).thenReturn(PROGRAMA_DTO);

        mvc.perform(put("/api/v1/decano/programas/" + PROGRAMA).contentType("application/json").content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombre").value("Sistemas"));

        final ArgumentCaptor<ActualizarProgramaDTO> captor = ArgumentCaptor.forClass(ActualizarProgramaDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(PROGRAMA, captor.getValue().id());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(COORDINADOR, captor.getValue().idCoordinador());
    }

    @Test
    void T_077_06_put_de_programa_ajeno_responde_404() throws Exception {
        when(actualizar.execute(any())).thenThrow(new ResourceNotFoundException("no existe"));

        mvc.perform(put("/api/v1/decano/programas/" + PROGRAMA).contentType("application/json").content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_con_cuerpo_invalido_responde_400() throws Exception {
        mvc.perform(put("/api/v1/decano/programas/" + PROGRAMA).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        verify(actualizar, never()).execute(any());
    }

    @Test
    void T_078_01_patch_estado_responde_200_con_el_estado_destino() throws Exception {
        when(cambiarEstado.execute(any())).thenReturn(PROGRAMA_DTO);

        mvc.perform(patch("/api/v1/decano/programas/" + PROGRAMA + "/estado").contentType("application/json")
                        .content("{\"estaActivo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<CambiarEstadoProgramaDTO> captor = ArgumentCaptor.forClass(CambiarEstadoProgramaDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertEquals(PROGRAMA, captor.getValue().id());
        assertEquals(Boolean.FALSE, captor.getValue().estaActivo());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void T_078_02_patch_con_grupos_activos_responde_409() throws Exception {
        when(cambiarEstado.execute(any())).thenThrow(new ConflictException("grupos activos"));

        mvc.perform(patch("/api/v1/decano/programas/" + PROGRAMA + "/estado").contentType("application/json")
                        .content("{\"estaActivo\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patch_sin_estado_responde_400() throws Exception {
        mvc.perform(patch("/api/v1/decano/programas/" + PROGRAMA + "/estado").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        verify(cambiarEstado, never()).execute(any());
    }
}
