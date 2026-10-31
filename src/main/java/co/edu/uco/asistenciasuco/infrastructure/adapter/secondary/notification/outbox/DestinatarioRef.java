package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.util.UUID;

/**
 * Referencia inmutable a un destinatario sin informacion de contacto personal.
 */
public record DestinatarioRef(String rol, UUID idUsuario) {
}
