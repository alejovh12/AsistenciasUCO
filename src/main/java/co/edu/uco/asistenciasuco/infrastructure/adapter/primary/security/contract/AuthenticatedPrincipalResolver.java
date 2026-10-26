package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.Set;
import java.util.UUID;

/**
 * Entrega la identidad institucional ya resuelta por la capa de seguridad: el UUID de {@code Usuario.id} y los
 * roles institucionales efectivos del token validado. Nunca acepta identificadores ni correos del cliente.
 */
public interface AuthenticatedPrincipalResolver {

    AuthenticatedPrincipal requireAuthenticatedPrincipal();

    record AuthenticatedPrincipal(UUID idUsuario, Set<InstitutionalRole> roles) {

        public AuthenticatedPrincipal {
            roles = Set.copyOf(roles);
        }
    }
}
