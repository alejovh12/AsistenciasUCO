package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports;

import java.util.UUID;

/** IDENT-001: puerto de entrada del reenvio del enlace de verificacion de correo. */
public interface ReenviarVerificacionCorreoInputPort {

    void execute(UUID actorUsuarioId, UUID grupoId, UUID estudianteId);
}
