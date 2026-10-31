package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 (hallazgo del envio real): un fallo de red no es un rechazo SMTP. El mensaje de conexion de Jakarta Mail
 * incluye el puerto ({@code ..., 587}) y la clasificacion anterior lo tomaba por un codigo 5xx, mandando a dead-letter un
 * corte transitorio de red. Solo una respuesta SMTP 5xx del servidor es un rechazo permanente.
 */
class JakartaMailCorreoTransportClasificacionTest {

    private static final String CONEXION_587 = "Couldn't connect to host, port: smtp.gmail.com, 587; timeout 10000";

    @Test
    void un_host_que_no_resuelve_con_puerto_587_es_transitorio() {
        final MessagingException conexion = new MessagingException(CONEXION_587, new UnknownHostException("smtp.gmail.com"));
        final MailSendException envio = new MailSendException("Mail server connection failed. Failed messages: " + CONEXION_587, conexion);

        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(envio));
    }

    @Test
    void conexion_rechazada_o_expirada_es_transitoria_aunque_el_mensaje_mencione_el_puerto() {
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new MailSendException("Mail server connection failed: " + CONEXION_587,
                        new MessagingException(CONEXION_587, new ConnectException("Connection refused: 587")))));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new MailSendException("Mail server connection failed: " + CONEXION_587,
                        new MessagingException(CONEXION_587, new SocketTimeoutException("Read timed out")))));
    }

    @Test
    void la_respuesta_4xx_del_servidor_es_transitoria() {
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new MessagingException("421 4.7.0 Try again later, closing connection.")));
    }

    @Test
    void la_respuesta_5xx_del_servidor_sigue_siendo_permanente() {
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new MessagingException("550 5.1.1 The email account that you tried to reach does not exist.")));
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new AuthenticationFailedException("535-5.7.8 Username and Password not accepted.")));
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new MailSendException("Mail server rejected", new MessagingException("554 5.7.1 Message rejected as spam."))));
    }
}
