package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.auth;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver.AuthenticatedPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/auth/me publica solo la identidad institucional ya resuelta por la capa de seguridad. */
class AuthControllerTest {

    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void devuelve_uuid_institucional_y_roles_en_orden_estable_sin_otros_campos() throws Exception {
        final AuthenticatedPrincipalResolver resolver = () -> new AuthenticatedPrincipal(
                USUARIO, Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.ADMINISTRADOR));

        mvc(resolver).perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idUsuario").value(USUARIO.toString()))
                .andExpect(jsonPath("$.datos.roles[0]").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.datos.roles[1]").value("DOCENTE"))
                .andExpect(jsonPath("$.datos.length()").value(2));
    }

    @Test
    void usuario_sin_roles_institucionales_recibe_lista_vacia() throws Exception {
        final AuthenticatedPrincipalResolver resolver = () -> new AuthenticatedPrincipal(USUARIO, Set.of());

        mvc(resolver).perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.roles.length()").value(0));
    }

    @Test
    void sin_principal_resoluble_responde_403() throws Exception {
        final AuthenticatedPrincipalResolver resolver = () -> {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        };

        mvc(resolver).perform(get("/api/v1/auth/me")).andExpect(status().isForbidden());
    }

    @Test
    void el_resolvedor_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new AuthController(null));
    }

    private static MockMvc mvc(final AuthenticatedPrincipalResolver resolver) {
        return MockMvcBuilders.standaloneSetup(new AuthController(resolver))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();
    }
}
