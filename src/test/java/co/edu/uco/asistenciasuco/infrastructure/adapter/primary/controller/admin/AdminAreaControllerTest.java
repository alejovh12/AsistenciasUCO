package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.ActualizarAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.CambiarEstadoAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.CrearAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto.CrearAreaDTO;
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

class AdminAreaControllerTest {

    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String BASE = "/api/v1/admin/areas";
    private static final String JSON = "application/json";

    private final ConsultarAreasInputPort consultar = mock(ConsultarAreasInputPort.class);
    private final CrearAreaInputPort crear = mock(CrearAreaInputPort.class);
    private final ActualizarAreaInputPort actualizar = mock(ActualizarAreaInputPort.class);
    private final CambiarEstadoAreaInputPort cambiarEstado = mock(CambiarEstadoAreaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminAreaController(consultar, crear, actualizar, cambiarEstado, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
            .build();

    private static AreaDTO tipo(final String nombre, final boolean activo) {
        return new AreaDTO(ID, "PRE", nombre, activo);
    }

    @Test
    void crear_responde_201_con_el_registro_releido_por_codigo() throws Exception {
        when(consultar.execute()).thenReturn(List.of(tipo("pregrado", true)));

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\" pre \",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.codigo").value("PRE"));

        final ArgumentCaptor<CrearAreaDTO> captor = ArgumentCaptor.forClass(CrearAreaDTO.class);
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

        final ArgumentCaptor<CrearAreaDTO> captor = ArgumentCaptor.forClass(CrearAreaDTO.class);
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
        when(consultar.execute()).thenReturn(List.of());

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\"PRE\",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_mapea_cuerpo_delega_y_devuelve_el_registro_releido() throws Exception {
        when(consultar.execute()).thenReturn(List.of(tipo("Nuevo", true)));

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombre").value("Nuevo"));

        final ArgumentCaptor<ActualizarAreaDTO> captor = ArgumentCaptor.forClass(ActualizarAreaDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(ID, captor.getValue().id());
        assertEquals("Nuevo", captor.getValue().nombre());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void actualizar_con_cuerpo_vacio_delega_nombre_nulo() throws Exception {
        when(consultar.execute()).thenReturn(List.of(tipo("x", true)));

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{}"))
                .andExpect(status().isOk());

        final ArgumentCaptor<ActualizarAreaDTO> captor = ArgumentCaptor.forClass(ActualizarAreaDTO.class);
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
        when(consultar.execute()).thenReturn(List.of());

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiar_estado_envia_el_estado_deseado_y_devuelve_el_registro() throws Exception {
        when(consultar.execute()).thenReturn(List.of(tipo("pregrado", false)));

        mvc.perform(patch(BASE + "/" + ID + "/estado").contentType(JSON).content("{\"activa\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.activa").value(false));

        final ArgumentCaptor<CambiarEstadoAreaDTO> captor = ArgumentCaptor.forClass(CambiarEstadoAreaDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertEquals(ID, captor.getValue().id());
        assertEquals(Boolean.FALSE, captor.getValue().activa());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void cambiar_estado_sin_valor_delega_nulo_y_si_no_aparece_responde_404() throws Exception {
        when(consultar.execute()).thenReturn(List.of());

        mvc.perform(patch(BASE + "/" + ID + "/estado").contentType(JSON).content("{}"))
                .andExpect(status().isNotFound());

        final ArgumentCaptor<CambiarEstadoAreaDTO> captor = ArgumentCaptor.forClass(CambiarEstadoAreaDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertNull(captor.getValue().activa());
    }
}
