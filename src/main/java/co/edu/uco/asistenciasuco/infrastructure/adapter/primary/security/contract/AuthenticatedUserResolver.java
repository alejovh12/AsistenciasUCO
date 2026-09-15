package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract;

import java.util.UUID;

public interface AuthenticatedUserResolver {

    UUID requireAuthenticatedUserId();
}
