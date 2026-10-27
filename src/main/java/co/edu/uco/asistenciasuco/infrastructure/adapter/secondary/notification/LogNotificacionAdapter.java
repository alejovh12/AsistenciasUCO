package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adaptador temporal de {@link NotificacionPort}: el componente institucional de notificaciones (correo, WhatsApp,
 * SMS) aun no esta integrado, asi que el aviso solo queda registrado en el log con su tipo, los roles e ids de los
 * destinatarios y los datos de trazabilidad. Nunca escribe correos, nombres ni el texto del mensaje.
 */
public final class LogNotificacionAdapter implements NotificacionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificacionAdapter.class);

    @Override
    public void notificar(final Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria.");
        LOGGER.info(describir(notificacion));
    }

    String describir(final Notificacion notificacion) {
        final String destinatarios = notificacion.destinatarios().stream()
                .map(destinatario -> destinatario.rol() + ":" + destinatario.idUsuario())
                .collect(Collectors.joining(","));
        final String datos = notificacion.datos().keySet().stream()
                .filter(clave -> !"motivo".equals(clave))
                .sorted()
                .map(clave -> clave + "=" + notificacion.datos().get(clave))
                .collect(Collectors.joining(","));
        return "NOTIFICACION_PENDIENTE_DE_CANAL tipo=" + notificacion.tipo()
                + " destinatarios=[" + destinatarios + "] datos=[" + datos + "]";
    }
}
