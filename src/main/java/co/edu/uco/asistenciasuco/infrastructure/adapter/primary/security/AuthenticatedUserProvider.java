package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;

import java.util.UUID;

public interface AuthenticatedUserProvider {

    UUID requireAuthenticatedUserId();
}
