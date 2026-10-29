package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.PerfilUsuarioProjection;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida de lectura del perfil del usuario autenticado (HU006). Solo el propio: no recibe otro id. */
public interface PerfilUsuarioQueryPort {

    Optional<PerfilUsuarioProjection> consultarPerfil(UUID usuarioEjecutor);
}
