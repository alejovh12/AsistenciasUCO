package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.ConsultarPerfilInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.PerfilUsuarioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** HU006: perfil del usuario autenticado (solo el propio). La edicion (PUT) sigue en {@code UsuarioController} (501). */
@RestController
@RequestMapping("/api/v1/usuarios")
public final class UsuarioPerfilController {

    private final ConsultarPerfilInputPort consultarPerfilInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UsuarioPerfilController(final ConsultarPerfilInputPort consultarPerfilInputPort,
                                   final AuthenticatedUserResolver authenticatedUserResolver) {
        this.consultarPerfilInputPort = Objects.requireNonNull(consultarPerfilInputPort, "ConsultarPerfilInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiDataResponse<PerfilUsuarioDTO>> consultarPerfil() {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                consultarPerfilInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId())));
    }
}
