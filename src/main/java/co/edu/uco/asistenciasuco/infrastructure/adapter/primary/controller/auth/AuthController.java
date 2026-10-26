package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.auth;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.auth.response.AuthMeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Identidad institucional del Bearer ya validado. La vinculación {@code issuer + subject → Usuario.id} (incluida
 * la primera, por correo verificado) ocurre al autenticar el token, antes de este endpoint; aquí solo se publica el
 * resultado. No recibe parámetros: ni UUID ni correo del cliente participan en la resolución.
 */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {

    private final AuthenticatedPrincipalResolver principalResolver;

    public AuthController(final AuthenticatedPrincipalResolver principalResolver) {
        this.principalResolver = Objects.requireNonNull(principalResolver, "AuthenticatedPrincipalResolver es obligatorio.");
    }

    @GetMapping("/me")
    public ResponseEntity<ApiDataResponse<AuthMeResponse>> consultarIdentidadInstitucional() {
        return ResponseEntity.ok(new ApiDataResponse<>(
                true,
                AuthMeResponse.from(principalResolver.requireAuthenticatedPrincipal())));
    }
}
