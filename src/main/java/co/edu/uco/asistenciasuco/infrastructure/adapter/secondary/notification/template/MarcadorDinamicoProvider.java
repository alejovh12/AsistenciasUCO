package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

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
}
