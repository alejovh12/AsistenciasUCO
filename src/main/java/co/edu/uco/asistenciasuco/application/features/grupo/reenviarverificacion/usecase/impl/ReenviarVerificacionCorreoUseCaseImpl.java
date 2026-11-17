package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.ReenviarVerificacionCorreoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
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
 * <p>
 * IDENT-001: reenvio del enlace al estudiante del grupo. IDENT-002: si su cuenta aun no tiene contrasena (registrado por el
 * docente o el administrador) se envia el aviso de activacion; si ya la tiene (matricula por QR), el de verificacion.
 */
public final class ReenviarVerificacionCorreoUseCaseImpl implements ReenviarVerificacionCorreoUseCase {

    private final VerificacionCorreoRepositoryPort repositorio;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final NotificacionPort notificacionPort;
    private final IdentityProviderPort identityProviderPort;

    public ReenviarVerificacionCorreoUseCaseImpl(
            final VerificacionCorreoRepositoryPort repositorio,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final NotificacionPort notificacionPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "VerificacionCorreoRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @Override
    public void execute(final UUID actorUsuarioId, final UUID grupoId, final UUID estudianteId) {
        if (actorUsuarioId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        if (grupoId == null || estudianteId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_053);
        }

        final UUID idUsuarioEstudiante = repositorio.consultarReenvio(grupoId, estudianteId, actorUsuarioId);
        final String nombre = usuarioRepositoryPort.consultarUsuarioPorId(idUsuarioEstudiante)
                .map(UsuarioIdentidadRepositoryProjection::primerNombre)
                .map(TextHelper::toNombrePropio)
                .orElse("");

        if (pendienteDeActivacion(idUsuarioEstudiante)) {
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_ACTIVACION_CUENTA,
                    "Active su cuenta en la plataforma de asistencia",
                    "Se registro una cuenta a su nombre en la plataforma de asistencia. Active su cuenta y defina su contrasena.",
                    List.of(new Notificacion.Destinatario(InstitutionalRole.ESTUDIANTE.name(), idUsuarioEstudiante, null, null)),
                    Map.of("nombre", nombre, "rol", "estudiante")
            ));
            return;
        }
        notificacionPort.notificar(new Notificacion(
                Notificacion.TIPO_VERIFICACION_CORREO,
                "Verifique su correo institucional",
                "Se registro una cuenta a su nombre en la plataforma de asistencia. Verifique su correo institucional.",
                List.of(new Notificacion.Destinatario(InstitutionalRole.ESTUDIANTE.name(), idUsuarioEstudiante, null, null)),
                nombre.isEmpty() ? Map.of() : Map.of("nombre", nombre)
        ));
    }

    /** Cuenta sin contrasena en el proveedor: si no se puede consultar, se conserva el aviso de verificacion. */
    private boolean pendienteDeActivacion(final UUID idUsuario) {
        try {
            return identityProviderPort.buscarCuentaPorIdUsuario(idUsuario)
                    .map(CuentaIdentidadDTO::idExterno)
                    .map(idExterno -> !identityProviderPort.tieneContrasena(idExterno))
                    .orElse(false);
        } catch (final IdentityProviderPort.IdentityProviderException _) {
            return false;
        }
    }
}
