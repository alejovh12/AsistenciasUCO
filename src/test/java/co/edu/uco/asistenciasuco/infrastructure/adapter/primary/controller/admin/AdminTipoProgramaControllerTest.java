package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.ActualizarTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.CambiarEstadoTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.CrearTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto.CrearTipoProgramaDTO;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTipoProgramaControllerTest {

    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String BASE = "/api/v1/admin/tipos-programa";
    private static final String JSON = "application/json";

    private final ConsultarTiposProgramaInputPort consultar = mock(ConsultarTiposProgramaInputPort.class);
    private final CrearTipoProgramaInputPort crear = mock(CrearTipoProgramaInputPort.class);
    private final ActualizarTipoProgramaInputPort actualizar = mock(ActualizarTipoProgramaInputPort.class);
    private final CambiarEstadoTipoProgramaInputPort cambiarEstado = mock(CambiarEstadoTipoProgramaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminTipoProgramaController(consultar, crear, actualizar, cambiarEstado, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
            .build();

    private static TipoProgramaDTO tipo(final String nombre, final boolean activo) {
        return new TipoProgramaDTO(ID, "PRE", nombre, activo);
    }

    @Test
    void listar_devuelve_todos_incluidos_inactivos() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("pregrado", true), tipo("otro", false)));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[1].activo").value(false));
    }

    @Test
    void crear_responde_201_con_el_registro_releido_por_codigo() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("pregrado", true)));

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\" pre \",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.codigo").value("PRE"));

        final ArgumentCaptor<CrearTipoProgramaDTO> captor = ArgumentCaptor.forClass(CrearTipoProgramaDTO.class);
        verify(crear).execute(captor.capture());
        assertEquals(" pre ", captor.getValue().codigo());
        assertEquals("pregrado", captor.getValue().nombre());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void crear_con_cuerpo_vacio_delega_nulos_y_el_caso_de_uso_responde_400() throws Exception {
        doThrow(new ValidationException("Codigo invalido")).when(crear).execute(any());

        mvc.perform(post(BASE).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest());

        final ArgumentCaptor<CrearTipoProgramaDTO> captor = ArgumentCaptor.forClass(CrearTipoProgramaDTO.class);
        verify(crear).execute(captor.capture());
        assertNull(captor.getValue().codigo());
    }

    @Test
    void crear_conflicto_se_traduce_a_409() throws Exception {
        doThrow(new ConflictException("Codigo duplicado")).when(crear).execute(any());

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\"PRE\",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_si_no_aparece_al_releer_responde_404() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of());

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\"PRE\",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_mapea_cuerpo_delega_y_devuelve_el_registro_releido() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("Nuevo", true)));

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombre").value("Nuevo"));

        final ArgumentCaptor<ActualizarTipoProgramaDTO> captor = ArgumentCaptor.forClass(ActualizarTipoProgramaDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(ID, captor.getValue().id());
        assertEquals("Nuevo", captor.getValue().nombre());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void actualizar_con_cuerpo_vacio_delega_nombre_nulo() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("x", true)));

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{}"))
                .andExpect(status().isOk());

        final ArgumentCaptor<ActualizarTipoProgramaDTO> captor = ArgumentCaptor.forClass(ActualizarTipoProgramaDTO.class);
        verify(actualizar).execute(captor.capture());
        assertNull(captor.getValue().nombre());
    }

    @Test
    void actualizar_conflicto_se_traduce_a_409() throws Exception {
        doThrow(new ConflictException("Nombre duplicado")).when(actualizar).execute(any());

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizar_si_no_aparece_al_releer_responde_404() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of());

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiar_estado_envia_el_estado_deseado_y_devuelve_el_registro() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("pregrado", false)));

        mvc.perform(patch(BASE + "/" + ID + "/estado").contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.activo").value(false));

        final ArgumentCaptor<CambiarEstadoTipoProgramaDTO> captor = ArgumentCaptor.forClass(CambiarEstadoTipoProgramaDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertEquals(ID, captor.getValue().id());
        assertEquals(Boolean.FALSE, captor.getValue().activo());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void cambiar_estado_sin_valor_delega_nulo_y_si_no_aparece_responde_404() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of());

        mvc.perform(patch(BASE + "/" + ID + "/estado").contentType(JSON).content("{}"))
                .andExpect(status().isNotFound());

        final ArgumentCaptor<CambiarEstadoTipoProgramaDTO> captor = ArgumentCaptor.forClass(CambiarEstadoTipoProgramaDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertNull(captor.getValue().activo());
    }
}
