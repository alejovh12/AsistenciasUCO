package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Puerto interno para resolver el correo y confirmacion de destinatarios al momento de enviar.
 */
public interface ContactoDestinatarioResolver {

    List<ContactoDestinatario> consultarContactos(Collection<UUID> idsUsuarios);
}
