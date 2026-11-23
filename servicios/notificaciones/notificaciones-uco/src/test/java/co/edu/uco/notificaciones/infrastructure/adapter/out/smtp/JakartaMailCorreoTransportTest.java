package co.edu.uco.notificaciones.infrastructure.adapter.out.smtp;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.domain.CorreoSaliente;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.infrastructure.config.NotificacionesProperties;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JakartaMailCorreoTransportTest {

    private GreenMail greenMail;
    private int puerto;

    @BeforeEach
    void arrancarSmtp() throws IOException {
        try (ServerSocket libre = new ServerSocket(0)) {
            puerto = libre.getLocalPort();
        }
        greenMail = new GreenMail(new ServerSetup(puerto, "127.0.0.1", "smtp"));
        greenMail.start();
    }

    @AfterEach
    void detenerSmtp() {
        greenMail.stop();
    }

    private JakartaMailCorreoTransport transporte(final String usuario) {
        final ConfiguracionCanal canal = new ConfiguracionCanal("smtp", "127.0.0.1", puerto, usuario, !usuario.isBlank(), "noreply@uco.edu.co", "UCO",
                false, false, 50, 60, 500, "", false, List.of());
        final NotificacionesProperties.Smtp smtp = new NotificacionesProperties.Smtp("127.0.0.1", puerto, usuario, "clave", false, false,
                "noreply@uco.edu.co", "UCO", Duration.ofSeconds(2), Duration.ofSeconds(2), 50, 60, 500, "",
                new NotificacionesProperties.TestMode(false, ""));
        return new JakartaMailCorreoTransport(canal, smtp);
    }

    private static CorreoSaliente correo(final String nombreRemitente) {
        return new CorreoSaliente("noreply@uco.edu.co", nombreRemitente, "noreply@uco.edu.co", List.of("a@uco.edu.co", "b@uco.edu.co"),
                "Asunto con ñ", "Texto plano", "<p>Hola</p>", "<notif-123-0@uco.edu.co>");
    }

    @Test
    void enviaMultipartConMessageIdFijoYDestinatariosEnBcc() throws Exception {
        final JakartaMailCorreoTransport transporte = transporte("");
        assertTrue(transporte.activo());
        transporte.enviar(correo("Asistencia UCO"));
        transporte.enviar(correo(""));
        final MimeMessage[] recibidos = greenMail.getReceivedMessages();
        assertEquals(6, recibidos.length);
        final MimeMessage primero = recibidos[0];
        assertEquals("Asunto con ñ", primero.getSubject());
        assertEquals("<notif-123-0@uco.edu.co>", primero.getMessageID());
        assertTrue(primero.getContent() instanceof MimeMultipart);
        assertTrue(primero.getFrom()[0].toString().contains("Asistencia UCO"));
        assertFalse(recibidos[3].getFrom()[0].toString().contains("Asistencia UCO"));
    }

    @Test
    void enviaSinBccCuandoNoHayLista() throws Exception {
        transporte("").enviar(new CorreoSaliente("noreply@uco.edu.co", "", "destino@uco.edu.co", List.of(), "A", "t", "<p>t</p>", "<id-1@uco.edu.co>"));
        assertEquals(1, greenMail.getReceivedMessages().length);
    }

    @Test
    void verificarConexionAbreYCierra() throws Exception {
        transporte("").verificarConexion();
        greenMail.stop();
        assertThrows(MessagingException.class, () -> transporte("").verificarConexion());
        greenMail.start();
    }

    @Test
    void construyeConAutenticacionCuandoHayUsuario() {
        assertTrue(transporte("usuario").activo());
    }

    @Test
    void unaRedCaidaEsTransitoria() {
        greenMail.stop();
        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> transporte("").enviar(correo("UCO")));
        assertTrue(ex.getMessage().contains("transitorio"));
        greenMail.start();
    }

    @Test
    void verificarConexionNoSoportadaConOtroSender() {
        final JakartaMailCorreoTransport otro = new JakartaMailCorreoTransport(mock(JavaMailSender.class));
        assertThrows(UnsupportedOperationException.class, otro::verificarConexion);
    }

    @Test
    void clasificaRechazosSmtp() throws Exception {
        final JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        final JakartaMailCorreoTransport transporte = new JakartaMailCorreoTransport(sender);

        doThrow(new MailSendException("550 5.1.1 usuario inexistente")).when(sender).send(any(MimeMessage.class));
        assertThrows(NoEntregableException.class, () -> transporte.enviar(correo("UCO")));

        doThrow(new MailSendException("timeout", new SocketTimeoutException("lento"))).when(sender).send(any(MimeMessage.class));
        assertThrows(IllegalStateException.class, () -> transporte.enviar(correo("UCO")));
    }

    @Test
    void esFalloPermanenteDistingueRedDeRechazo() {
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(new RuntimeException("x", new AddressException("mala"))));
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(new MailSendException("smtp: 553 no permitido")));
        final Address[] invalidas = {new jakarta.mail.internet.InternetAddress()};
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(new SendFailedException("fallo", null, new Address[0], new Address[0], invalidas)));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(new SendFailedException("fallo", null, new Address[0], invalidas, invalidas)));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(new RuntimeException("Couldn't connect to host, port: smtp, 587", new ConnectException("587"))));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(new RuntimeException("x", new UnknownHostException("h"))));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(new RuntimeException("sin codigo")));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(new RuntimeException()));
    }
}
