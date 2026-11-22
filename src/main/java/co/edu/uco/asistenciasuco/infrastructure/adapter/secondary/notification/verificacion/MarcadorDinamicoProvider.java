package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resuelve al momento de la entrega los marcadores que no pueden vivir en el outbox (valores secretos o de un solo
 * uso, como el enlace de verificacion de correo). Para los tipos que no los necesitan devuelve un mapa vacio. Un fallo
 * permanente se senala con {@code NotificacionNoEntregableException}; cualquier otra excepcion se trata como reintentable.
 */
@FunctionalInterface
public interface MarcadorDinamicoProvider {

    /** Proveedor que no aporta marcadores. */
    MarcadorDinamicoProvider NINGUNO = (tipo, idsUsuarios) -> Map.of();

    Map<String, String> resolver(String tipo, List<UUID> idsUsuarios) throws NotificacionNoEntregableException;

    /**
     * Igual que {@link #resolver(String, List)} pero identifica el aviso del outbox, para que un reintento del mismo aviso
     * reciba los mismos marcadores (por ejemplo el mismo enlace) y no emita secretos nuevos que invaliden los ya entregados.
     */
    default Map<String, String> resolver(final UUID idNotificacion, final String tipo, final List<UUID> idsUsuarios)
            throws NotificacionNoEntregableException {
        return resolver(tipo, idsUsuarios);
    }
}
