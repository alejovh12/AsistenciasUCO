package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact;

import java.util.Objects;
import java.util.UUID;

/**
 * Contacto de destinatario resuelto desde la tabla Usuario al momento de entregar la notificacion.
 */
public record ContactoDestinatario(
        UUID idUsuario,
        String correo,
        boolean correoConfirmado
) {

    public ContactoDestinatario {
        Objects.requireNonNull(idUsuario, "El idUsuario del contacto es obligatorio.");
        Objects.requireNonNull(correo, "El correo del contacto es obligatorio.");
        correo = correo.trim();
    }
}
