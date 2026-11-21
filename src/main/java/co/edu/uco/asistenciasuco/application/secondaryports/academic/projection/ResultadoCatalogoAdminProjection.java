package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Resultado de crear, actualizar o cambiar el estado de un registro de catalogo administrado: id y mensaje de usuario de la DB (CAT_007/CAT_008). */
public record ResultadoCatalogoAdminProjection(UUID id, String mensajeUsuario) {
}
