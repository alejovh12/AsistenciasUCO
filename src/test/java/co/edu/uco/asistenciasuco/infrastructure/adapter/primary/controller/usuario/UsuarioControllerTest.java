package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.ProvisionarUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private final ProvisionarUsuarioInputPort provisionarUsuario = mock(ProvisionarUsuarioInputPort.class);
    private final UsuarioController controller = new UsuarioController(provisionarUsuario);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void consultarPerfil_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class,
                        controller::consultarPerfilUsuarioAutenticado).getCode());
    }

    @Test
    void actualizarPerfil_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class,
                        () -> controller.actualizarPerfilUsuarioAutenticado(new Object())).getCode());
    }

    @Test
    void consultarPerfil_via_http_retorna_501() throws Exception {
        mvc.perform(get("/api/v1/usuarios/perfil")).andExpect(status().isNotImplemented());
    }

    @Test
    void actualizarPerfil_via_http_retorna_501() throws Exception {
        mvc.perform(put("/api/v1/usuarios/perfil").contentType("application/json").content("{}"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void crearUsuario_con_datos_validos_delega_en_el_puerto_y_retorna_201() throws Exception {
        final UUID usuarioId = UUID.randomUUID();
        when(provisionarUsuario.execute(org.mockito.ArgumentMatchers.any(ProvisionarUsuarioDTO.class)))
                .thenReturn(new ProvisionarUsuarioResultadoDTO(usuarioId, true, "Usuario creado."));
        final String body = "{\"tipoIdIdentificacion\":\"" + UUID.randomUUID() + "\",\"numeroIdentificacion\":123456789,"
                + "\"primerNombre\":\"Ana\",\"segundoNombre\":\"Maria\",\"primerApellido\":\"Perez\","
                + "\"segundoApellido\":\"Gomez\",\"correo\":\"ana@uco.edu.co\",\"password\":\"Clave123!\"}";

        mvc.perform(post("/api/v1/usuarios").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Usuario creado."));

        final ArgumentCaptor<ProvisionarUsuarioDTO> captor = ArgumentCaptor.forClass(ProvisionarUsuarioDTO.class);
        verify(provisionarUsuario).execute(captor.capture());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals("ana@uco.edu.co", captor.getValue().correo());
    }

    @Test
    void crearUsuario_con_datos_invalidos_retorna_400() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
