package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedPrincipalResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Resuelve el UUID institucional del usuario autenticado a partir del principal ya
 * normalizado por {@link InstitutionalJwtAuthenticationConverter}.
 *
 * <p>Deliberadamente NO lee claims del {@code Jwt}, no conoce el nombre del claim
 * {@code idUsuario}, no conoce Keycloak ni ningún proveedor, y no consulta la base de datos:
 * el converter ya dejó el principal normalizado como {@code idUsuario.toString()} en
 * {@link Authentication#getName()} y los roles institucionales como authorities {@code ROLE_*}.</p>
 */
@Service
public final class SecurityContextAuthenticatedUserResolver
        implements AuthenticatedUserResolver, AuthenticatedPrincipalResolver {

    private static final String SPRING_AUTHORITY_PREFIX = "ROLE_";

    @Override
    public UUID requireAuthenticatedUserId() {
        return requireAuthentication().userId();
    }

    @Override
    public AuthenticatedPrincipal requireAuthenticatedPrincipal() {
        final Resolved resolved = requireAuthentication();
        return new AuthenticatedPrincipal(resolved.userId(), resolved.roles());
    }

    private Resolved requireAuthentication() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        final String principal = authentication.getName();
        if (principal == null || principal.isBlank()) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }

        final UUID userId;
        try {
            userId = UUID.fromString(principal);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("El principal autenticado no tiene formato UUID valido.");
        }
        return new Resolved(userId, rolesOf(authentication));
    }

    private static Set<InstitutionalRole> rolesOf(final Authentication authentication) {
        final Set<InstitutionalRole> roles = EnumSet.noneOf(InstitutionalRole.class);
        for (final GrantedAuthority authority : authentication.getAuthorities()) {
            final String value = authority.getAuthority();
            if (value == null || !value.startsWith(SPRING_AUTHORITY_PREFIX)) {
                continue;
            }
            final String name = value.substring(SPRING_AUTHORITY_PREFIX.length());
            Arrays.stream(InstitutionalRole.values())
                    .filter(role -> role.name().equals(name))
                    .findFirst()
                    .ifPresent(roles::add);
        }
        return roles;
    }

    private record Resolved(UUID userId, Set<InstitutionalRole> roles) {
    }
}
