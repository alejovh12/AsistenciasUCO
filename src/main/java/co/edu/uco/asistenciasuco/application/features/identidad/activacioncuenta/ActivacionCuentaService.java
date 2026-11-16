package co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-002: piezas comunes de la activacion de cuentas registradas por otra persona (decano, coordinador, docente o
 * estudiante). El registro ya no recibe contrasena: la cuenta nace sin credencial en el proveedor de identidad y su
 * titular la define con el enlace del aviso {@code ACTIVACION_CUENTA}.
 *
 * <p>POJO sin anotaciones de framework: se cablea con {@code @Bean} en la configuracion de infraestructura.</p>
 */
public final class ActivacionCuentaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivacionCuentaService.class);
    private static final int BYTES_SECRETO = 32;
    static final String ASUNTO = "Active su cuenta en la plataforma de asistencia";
    static final String MENSAJE = "Se registro una cuenta a su nombre en la plataforma de asistencia. Active su cuenta y defina su contrasena.";

    private final NotificacionPort notificacionPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SecureRandom random;

    public ActivacionCuentaService(
            final NotificacionPort notificacionPort,
            final PasswordEncoderPort passwordEncoderPort,
            final SecureRandom random
    ) {
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.random = Objects.requireNonNull(random, "SecureRandom es obligatorio.");
    }

    /**
     * Valor para la columna legada {@code Usuario.password} de un alta sin contrasena: el hash de un secreto aleatorio que
     * nadie conoce (la autenticacion vive en el proveedor de identidad).
     */
    public String credencialLegadaInutilizable() {
        final byte[] secreto = new byte[BYTES_SECRETO];
        random.nextBytes(secreto);
        return passwordEncoderPort.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(secreto));
    }

    /**
     * Registra el aviso de activacion (el enlace lo emite el canal al entregar). De mejor esfuerzo: un fallo del outbox
     * nunca revierte el alta ya confirmada; el responsable puede reenviar el enlace.
     */
    public void avisar(final UUID idUsuario, final String primerNombre, final InstitutionalRole rol) {
        Objects.requireNonNull(idUsuario, "El usuario a activar es obligatorio.");
        Objects.requireNonNull(rol, "El rol de la cuenta es obligatorio.");
        try {
            notificacionPort.notificar(notificacion(idUsuario, primerNombre, rol));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible registrar el aviso de activacion de la cuenta del usuario {}: {}",
                    idUsuario, exception.getClass().getSimpleName());
        }
    }

    /** Aviso de activacion con los marcadores de la plantilla (nombre y rol; el enlace lo agrega el canal). */
    static Notificacion notificacion(final UUID idUsuario, final String primerNombre, final InstitutionalRole rol) {
        final String nombre = primerNombre == null ? "" : TextHelper.toNombrePropio(primerNombre);
        return new Notificacion(
                Notificacion.TIPO_ACTIVACION_CUENTA,
                ASUNTO,
                MENSAJE,
                List.of(new Notificacion.Destinatario(rol.name(), idUsuario, null, null)),
                Map.of("nombre", nombre, "rol", descripcionRol(rol))
        );
    }

    static String descripcionRol(final InstitutionalRole rol) {
        return switch (rol) {
            case ADMINISTRADOR -> "administrador(a) institucional";
            case DECANO -> "decano(a) de facultad";
            case COORDINADOR -> "coordinador(a) de programa";
            case DOCENTE -> "docente";
            case ESTUDIANTE -> "estudiante";
        };
    }
}
