package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.GestionarCategoriaJustificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.CategoriaJustificacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.GuardarCategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

class AdminCategoriaJustificacionControllerTest {

    private static final UUID ACTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RUTA = "/api/v1/admin/categorias-justificacion";

    private final GestionarCategoriaJustificacionInputPort port = mock(GestionarCategoriaJustificacionInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminCategoriaJustificacionController(port, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void lista_todos_los_registros() throws Exception {
        when(port.consultar()).thenReturn(List.of(new CategoriaJustificacionAdminDTO(ID, "QA1", "Nombre", "desc", true, true)));
        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].estaActivo").value(true))
                .andExpect(jsonPath("$.datos[0].codigo").value("QA1"));
    }

    @Test
    void crea_con_201_y_el_mensaje_de_la_db() throws Exception {
        when(port.crear(new GuardarCategoriaJustificacionDTO(null, "QA1", "Nombre", "d", true, ACTOR))).thenReturn(new ResultadoCatalogoAdminDTO(ID, "Guardado."));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"QA1\",\"nombre\":\"Nombre\",\"descripcion\":\"d\",\"requiereSoporte\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.id").value(ID.toString()))
                .andExpect(jsonPath("$.mensajeUsuario").value("Guardado."));
    }

    @Test
    void actualiza_con_200() throws Exception {
        when(port.actualizar(new GuardarCategoriaJustificacionDTO(ID, null, "Nombre", "d", false, ACTOR))).thenReturn(new ResultadoCatalogoAdminDTO(ID, "Guardado."));
        mvc.perform(put(RUTA + "/" + ID).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nombre\",\"descripcion\":\"d\",\"requiereSoporte\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.id").value(ID.toString()));
    }

    @Test
    void cambia_el_estado() throws Exception {
        when(port.cambiarEstado(ID, false, ACTOR)).thenReturn(new ResultadoCatalogoAdminDTO(ID, "Inactivo."));
        mvc.perform(patch(RUTA + "/" + ID + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estaActivo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estaActivo").value(false))
                .andExpect(jsonPath("$.mensajeUsuario").value("Inactivo."));
        verify(port).cambiarEstado(ID, false, ACTOR);
    }

    @Test
    void cuerpo_invalido_es_400() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"qa\",\"nombre\":\"<b>\",\"requiereSoporte\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(RUTA + "/" + ID + "/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazos_de_la_db_se_traducen() throws Exception {
        doThrow(new ForbiddenException(SecurityErrorCode.FORBIDDEN)).when(port).cambiarEstado(any(), any(Boolean.class), any());
        mvc.perform(patch(RUTA + "/" + ID + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estaActivo\":true}"))
                .andExpect(status().isForbidden());
        doThrow(new ConflictException(CatalogoErrorCode.CTG_003)).when(port).cambiarEstado(any(), any(Boolean.class), any());
        mvc.perform(patch(RUTA + "/" + ID + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estaActivo\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void constructor_rechaza_nulos() {
        assertThrows(NullPointerException.class, () -> new AdminCategoriaJustificacionController(null, identity));
        assertThrows(NullPointerException.class, () -> new AdminCategoriaJustificacionController(port, null));
    }
}
