package co.edu.uco.notificaciones.infrastructure.adapter.out.smtp;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.domain.CorreoSaliente;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.infrastructure.config.NotificacionesProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/** Transporte SMTP sobre Jakarta Mail / Spring Mail. Distingue rechazo permanente (5xx, direccion invalida) de fallo transitorio. */
public class JakartaMailCorreoTransport implements CorreoTransportPort {

    private static final Pattern CODIGO_SMTP_5XX = Pattern.compile("(?<!\\d)5\\d{2}(?!\\d)");

    private final JavaMailSender mailSender;

    public JakartaMailCorreoTransport(final ConfiguracionCanal canal, final NotificacionesProperties.Smtp smtp) {
        this(construir(Objects.requireNonNull(canal), Objects.requireNonNull(smtp)));
    }

    public JakartaMailCorreoTransport(final JavaMailSender mailSender) {
        this.mailSender = Objects.requireNonNull(mailSender, "JavaMailSender es obligatorio.");
    }

    private static JavaMailSender construir(final ConfiguracionCanal canal, final NotificacionesProperties.Smtp smtp) {
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(canal.host());
        sender.setPort(canal.port());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        final boolean conAutenticacion = !canal.username().isBlank();
        if (conAutenticacion) {
            sender.setUsername(canal.username());
            sender.setPassword(smtp.password());
        }
        final Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(conAutenticacion));
        props.put("mail.smtp.starttls.enable", String.valueOf(canal.starttls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(canal.ssl()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtp.connectTimeout().toMillis()));
        props.put("mail.smtp.timeout", String.valueOf(smtp.readTimeout().toMillis()));
        props.put("mail.smtp.writetimeout", String.valueOf(smtp.readTimeout().toMillis()));
        props.put("mail.smtp.localhost", "localhost");
        return sender;
    }

    @Override
    public boolean activo() {
        return true;
    }

    @Override
    public void verificarConexion() throws MessagingException {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) {
            throw new UnsupportedOperationException("El JavaMailSender configurado no permite verificar la conexion.");
        }
        impl.testConnection();
    }

    @Override
    public void enviar(final CorreoSaliente correo) {
        Objects.requireNonNull(correo, "CorreoSaliente es obligatorio.");
        try {
            final MimeMessage base = mailSender.createMimeMessage();
            final MimeMessage message = new IdFijoMimeMessage(base.getSession(), correo.messageId());
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (correo.fromName().isBlank()) {
                helper.setFrom(correo.fromAddress());
            } else {
                helper.setFrom(correo.fromAddress(), correo.fromName());
            }
            helper.setTo(correo.toAddress());
            if (!correo.bccAddresses().isEmpty()) {
                helper.setBcc(correo.bccAddresses().toArray(String[]::new));
            }
            helper.setSubject(correo.asunto());
            helper.setText(correo.textoPlano(), correo.html());
            mailSender.send(message);
        } catch (final MailException | MessagingException | UnsupportedEncodingException ex) {
            if (esFalloPermanente(ex)) {
                throw new NoEntregableException("Rechazo SMTP permanente al enviar notificacion.");
            }
            throw new IllegalStateException("Fallo SMTP transitorio al enviar notificacion.", ex);
        }
    }

    static boolean esFalloPermanente(final Throwable error) {
        // Un fallo de red (host que no resuelve, conexion rechazada o expirada) es transitorio. Su mensaje incluye el puerto
        // (por ejemplo "..., 587") y no debe confundirse con una respuesta SMTP 5xx del servidor.
        for (Throwable red = error; red != null; red = red.getCause()) {
            if (red instanceof java.net.UnknownHostException
                    || red instanceof java.net.ConnectException
                    || red instanceof java.net.SocketException
                    || red instanceof java.net.SocketTimeoutException
                    || red instanceof java.io.InterruptedIOException
                    || red instanceof org.eclipse.angus.mail.util.MailConnectException) {
                return false;
            }
        }
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof AddressException) {
                return true;
            }
            if (cursor instanceof SendFailedException sfe
                    && sfe.getInvalidAddresses() != null
                    && sfe.getInvalidAddresses().length > 0
                    && (sfe.getValidUnsentAddresses() == null || sfe.getValidUnsentAddresses().length == 0)) {
                return true;
            }
            final String msg = cursor.getMessage();
            if (msg != null && CODIGO_SMTP_5XX.matcher(msg).find()) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /** Fija el Message-ID para que un reintento del mismo bloque lleve el mismo identificador. */
    private static final class IdFijoMimeMessage extends MimeMessage {
        private final String messageId;

        private IdFijoMimeMessage(final Session session, final String messageId) {
            super(session);
            this.messageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
        }
    }
}
