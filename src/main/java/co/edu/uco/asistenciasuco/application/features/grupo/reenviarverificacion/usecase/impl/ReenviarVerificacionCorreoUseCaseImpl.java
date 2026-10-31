package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.ReenviarVerificacionCorreoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: la base de datos decide si el reenvio procede (ambito del docente titular o coordinador, correo aun sin
 * verificar y espera minima). Solo entonces se registra el aviso, sin correo ni token: el enlace nuevo se emite al
 * entregar y revoca el anterior. A diferencia del aviso del registro, aqui el reenvio es la accion pedida, asi que un
 * fallo de la cola se propaga.
 */
public final class ReenviarVerificacionCorreoUseCaseImpl implements ReenviarVerificacionCorreoUseCase {

    private final VerificacionCorreoRepositoryPort repositorio;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final NotificacionPort notificacionPort;

    public ReenviarVerificacionCorreoUseCaseImpl(
            final VerificacionCorreoRepositoryPort repositorio,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final NotificacionPort notificacionPort
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "VerificacionCorreoRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public void execute(final UUID actorUsuarioId, final UUID grupoId, final UUID estudianteId) {
        if (actorUsuarioId == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        if (grupoId == null || estudianteId == null) {
            throw new ValidationException("El grupo y el estudiante son obligatorios.");
        }

        final UUID idUsuarioEstudiante = repositorio.consultarReenvio(grupoId, estudianteId, actorUsuarioId);
        final String nombre = usuarioRepositoryPort.consultarUsuarioPorId(idUsuarioEstudiante)
                .map(UsuarioIdentidadRepositoryProjection::primerNombre)
                .map(TextHelper::toNombrePropio)
                .orElse("");

        notificacionPort.notificar(new Notificacion(
                Notificacion.TIPO_VERIFICACION_CORREO,
                "Verifique su correo institucional",
                "Se registro una cuenta a su nombre en la plataforma de asistencia. Verifique su correo institucional.",
                List.of(new Notificacion.Destinatario(InstitutionalRole.ESTUDIANTE.name(), idUsuarioEstudiante, null, null)),
                nombre.isEmpty() ? Map.of() : Map.of("nombre", nombre)
        ));
    }
}
