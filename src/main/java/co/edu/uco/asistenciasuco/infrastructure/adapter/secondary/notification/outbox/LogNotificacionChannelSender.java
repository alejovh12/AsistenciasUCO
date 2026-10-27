package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sender de canal por defecto que registra las notificaciones outbox en el log
 * sin exponer datos confidenciales ni de contacto personal.
 */
public class LogNotificacionChannelSender implements NotificacionChannelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificacionChannelSender.class);

    @Override
    public void enviar(final NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria.");
        LOGGER.info(describir(notificacion));
    }

    String describir(final NotificacionPersistida notificacion) {
        final String destinatarios = notificacion.destinatarios().stream()
                .map(d -> d.rol() + ":" + d.idUsuario())
                .collect(Collectors.joining(","));
        final String datos = notificacion.datos().keySet().stream()
                .filter(clave -> !"motivo".equals(clave))
                .sorted()
                .map(clave -> clave + "=" + notificacion.datos().get(clave))
                .collect(Collectors.joining(","));
        return "OUTBOX_NOTIFICACION_DESPACHADA id=" + notificacion.id()
                + " tipo=" + notificacion.tipo()
                + " destinatarios=[" + destinatarios + "] datos=[" + datos + "]";
    }
}
