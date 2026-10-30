package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import java.util.List;
import java.util.Objects;

/**
 * Mensaje de correo electronico saliente listo para ser transmitido por el transporte SMTP.
 */
public record CorreoSaliente(
        String fromAddress,
        String fromName,
        String toAddress,
        List<String> bccAddresses,
        String asunto,
        String textoPlano,
        String html,
        String messageId
) {

    public CorreoSaliente {
        fromAddress = Objects.requireNonNull(fromAddress, "fromAddress es obligatorio.").trim();
        fromName = fromName == null ? "" : fromName.trim();
        toAddress = Objects.requireNonNull(toAddress, "toAddress es obligatorio.").trim();
        bccAddresses = bccAddresses == null ? List.of() : List.copyOf(bccAddresses);
        asunto = Objects.requireNonNull(asunto, "asunto es obligatorio.");
        textoPlano = Objects.requireNonNull(textoPlano, "textoPlano es obligatorio.");
        html = Objects.requireNonNull(html, "html es obligatorio.");
        messageId = Objects.requireNonNull(messageId, "messageId es obligatorio.").trim();
    }
}
