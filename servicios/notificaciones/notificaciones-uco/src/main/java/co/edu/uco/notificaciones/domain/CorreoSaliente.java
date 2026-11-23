package co.edu.uco.notificaciones.domain;

import java.util.List;
import java.util.Objects;

/** Mensaje listo para el transporte SMTP: el remitente va en To y los destinatarios reales en BCC. */
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
        Objects.requireNonNull(asunto, "asunto es obligatorio.");
        Objects.requireNonNull(textoPlano, "textoPlano es obligatorio.");
        Objects.requireNonNull(html, "html es obligatorio.");
        messageId = Objects.requireNonNull(messageId, "messageId es obligatorio.").trim();
    }
}
