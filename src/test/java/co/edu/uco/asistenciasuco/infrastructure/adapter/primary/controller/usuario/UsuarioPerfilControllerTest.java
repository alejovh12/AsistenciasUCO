package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.ConsultarPerfilInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.PerfilUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.VinculoUsuarioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU006: GET /api/v1/usuarios/perfil (PC-01..PC-03). */
class UsuarioPerfilControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConsultarPerfilInputPort consultar = mock(ConsultarPerfilInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new UsuarioPerfilController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void PC01_devuelve_el_perfil_del_usuario_autenticado() throws Exception {
        when(consultar.execute(ACTOR)).thenReturn(new PerfilUsuarioDTO(ACTOR, "CC", "Cedula", 123, "Ana", null, "Perez", null,
                "Ana Perez", "ana@uco.edu.co", true, true,
                List.of(new VinculoUsuarioDTO("ESTUDIANTE", UUID.randomUUID(), "Ingenieria", "Facultad", true))));

        mvc.perform(get("/api/v1/usuarios/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idUsuario").value(ACTOR.toString()))
                .andExpect(jsonPath("$.datos.primerNombre").value("Ana"))
                .andExpect(jsonPath("$.datos.correo").value("ana@uco.edu.co"))
                .andExpect(jsonPath("$.datos.numeroIdentificacion").value(123))
                .andExpect(jsonPath("$.datos.vinculos[0].rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.datos.password").doesNotExist());
    }

    @Test
    void PC02_perfil_inexistente_responde_404() throws Exception {
        when(consultar.execute(ACTOR)).thenThrow(new ResourceNotFoundException("No se encontro el perfil del usuario autenticado."));

        mvc.perform(get("/api/v1/usuarios/perfil")).andExpect(status().isNotFound());
    }

    @Test
    void PC03_constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new UsuarioPerfilController(null, identity));
        assertThrows(NullPointerException.class, () -> new UsuarioPerfilController(consultar, null));
    }
}
