package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase;

import java.util.UUID;

/** IDENT-001: reenvia el enlace de verificacion al estudiante cuyo correo aun no se verifica. */
public interface ReenviarVerificacionCorreoUseCase {

    void execute(UUID actorUsuarioId, UUID grupoId, UUID estudianteId);
}
