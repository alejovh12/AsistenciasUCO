package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.auth.response;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver.AuthenticatedPrincipal;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Identidad institucional resuelta por el backend: solo el UUID de {@code Usuario.id} y sus roles efectivos. */
public record AuthMeResponse(UUID idUsuario, List<String> roles) {

    public static AuthMeResponse from(final AuthenticatedPrincipal principal) {
        final List<String> roles = Arrays.stream(InstitutionalRole.values())
                .filter(principal.roles()::contains)
                .map(Enum::name)
                .toList();
        return new AuthMeResponse(principal.idUsuario(), roles);
    }
}
