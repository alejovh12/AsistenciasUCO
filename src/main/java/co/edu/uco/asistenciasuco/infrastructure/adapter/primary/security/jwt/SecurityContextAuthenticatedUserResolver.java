package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resuelve el UUID institucional del usuario autenticado a partir del principal ya
 * normalizado por {@link InstitutionalJwtAuthenticationConverter}.
 *
 * <p>Deliberadamente NO lee claims del {@code Jwt}, no conoce el nombre del claim
 * {@code idUsuario}, no conoce Keycloak ni ningún proveedor, y no consulta la base de datos:
 * el converter ya dejó el principal normalizado como {@code idUsuario.toString()} en
 * {@link Authentication#getName()}.</p>
 */
@Service
public final class SecurityContextAuthenticatedUserResolver implements AuthenticatedUserResolver {

    @Override
    public UUID requireAuthenticatedUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        final String principal = authentication.getName();
        if (principal == null || principal.isBlank()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("El principal autenticado no tiene formato UUID valido.");
        }
    }
}
