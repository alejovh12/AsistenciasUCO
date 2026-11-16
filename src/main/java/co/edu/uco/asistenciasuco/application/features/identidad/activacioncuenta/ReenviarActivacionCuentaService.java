package co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.primaryports.ReenviarActivacionCuentaInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * IDENT-002: reenvia el enlace de activacion de una cuenta registrada por otra persona. Orden:
 * <ol>
 *   <li>el correo debe existir ({@code NOF_APP_010}) y estar en el alcance del actor ({@code ACC_APP_013});</li>
 *   <li>la base de datos valida el perfil del actor, que la cuenta siga sin activar ({@code ACT_002}) y la espera
 *       minima entre envios ({@code VCE_003});</li>
 *   <li>se registra el aviso {@code ACTIVACION_CUENTA} (el canal emite un enlace nuevo y revoca el anterior).</li>
 * </ol>
 */
public final class ReenviarActivacionCuentaService implements ReenviarActivacionCuentaInputPort {

    static final String CODIGO_EXITO = "ACT_001";
    /** Rol que se nombra en el aviso cuando la persona tiene varios: el de mayor responsabilidad. */
    private static final List<InstitutionalRole> PRIORIDAD = List.of(
            InstitutionalRole.ADMINISTRADOR, InstitutionalRole.DECANO, InstitutionalRole.COORDINADOR,
            InstitutionalRole.DOCENTE, InstitutionalRole.ESTUDIANTE);

    private final InstitutionalScopePort scope;
    private final VerificacionCorreoRepositoryPort verificaciones;
    private final UsuarioRepositoryPort usuarios;
    private final NotificacionPort notificacionPort;
    private final MessageCatalogPort mensajes;

    public ReenviarActivacionCuentaService(
            final InstitutionalScopePort scope,
            final VerificacionCorreoRepositoryPort verificaciones,
            final UsuarioRepositoryPort usuarios,
            final NotificacionPort notificacionPort,
            final MessageCatalogPort mensajes
    ) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.verificaciones = Objects.requireNonNull(verificaciones, "VerificacionCorreoRepositoryPort es obligatorio.");
        this.usuarios = Objects.requireNonNull(usuarios, "UsuarioRepositoryPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public String reenviar(final UUID actorUsuario, final String correo) {
        Objects.requireNonNull(actorUsuario, "El usuario autenticado es obligatorio.");
        if (correo == null || correo.isBlank()) {
            throw new ResourceNotFoundException(MensajeUsuarioErrorCode.NOF_APP_010);
        }
        final UUID destino = scope.findUsuarioIdByEmail(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException(MensajeUsuarioErrorCode.NOF_APP_010));
        if (!scope.canGestionarCuenta(actorUsuario, destino)) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_013);
        }
        final UUID idUsuario = verificaciones.consultarReenvioActivacion(destino, actorUsuario);
        final String primerNombre = usuarios.consultarUsuarioPorId(idUsuario)
                .map(UsuarioIdentidadRepositoryProjection::primerNombre)
                .orElse(null);
        notificacionPort.notificar(ActivacionCuentaService.notificacion(idUsuario, primerNombre, rolPrincipal(idUsuario)));
        return mensajes.getUserMessage(CODIGO_EXITO);
    }

    private InstitutionalRole rolPrincipal(final UUID idUsuario) {
        final Set<InstitutionalRole> roles = scope.findRolesActivosByUsuario(idUsuario);
        return PRIORIDAD.stream().filter(roles::contains).findFirst().orElse(InstitutionalRole.ESTUDIANTE);
    }
}
