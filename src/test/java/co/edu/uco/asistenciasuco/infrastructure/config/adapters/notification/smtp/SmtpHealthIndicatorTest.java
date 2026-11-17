package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.CorreoTransport;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.JakartaMailCorreoTransport;
import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** OBS-10 (VAL-oleada8): indicador de salud del SMTP. */
class SmtpHealthIndicatorTest {

    @Test
    void conexion_correcta_da_up_con_host_y_puerto() {
        final Health salud = new SmtpHealthIndicator(transporte(() -> { }), "smtp.uco.edu.co", 587).health();

        assertEquals(Status.UP, salud.getStatus());
        assertEquals("smtp.uco.edu.co", salud.getDetails().get("host"));
        assertEquals(587, salud.getDetails().get("port"));
    }

    @Test
    void fallo_de_conexion_da_down_con_el_tipo_de_error_y_sin_su_mensaje() {
        final Health salud = new SmtpHealthIndicator(
                transporte(() -> { throw new AuthenticationFailedException("535 usuario notificaciones@uco.edu.co rechazado"); }),
                "smtp.uco.edu.co", 587).health();

        assertEquals(Status.DOWN, salud.getStatus());
        assertEquals("AuthenticationFailedException", salud.getDetails().get("error"));
        assertFalse(salud.getDetails().toString().contains("notificaciones@uco.edu.co"));
    }

    @Test
    void transporte_que_no_sabe_verificar_da_unknown() {
        final CorreoTransport sinVerificacion = correo -> { };

        assertEquals(Status.UNKNOWN, new SmtpHealthIndicator(sinVerificacion, "smtp", 25).health().getStatus());
    }

    @Test
    void transporte_jakarta_con_un_sender_que_no_es_jakarta_no_verifica() {
        final JakartaMailCorreoTransport transporte = new JakartaMailCorreoTransport(
                org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class));

        assertThrows(UnsupportedOperationException.class, transporte::verificarConexion);
    }

    @Test
    void transporte_jakarta_real_contra_un_servidor_smtp_local_que_saluda_da_up() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            final Thread atencion = Thread.ofVirtual().start(() -> atenderSmtpMinimo(servidor));
            final JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost("127.0.0.1");
            sender.setPort(servidor.getLocalPort());
            sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "2000");
            sender.getJavaMailProperties().put("mail.smtp.timeout", "2000");

            final Health salud = new SmtpHealthIndicator(new JakartaMailCorreoTransport(sender), "127.0.0.1",
                    servidor.getLocalPort()).health();

            assertEquals(Status.UP, salud.getStatus());
            atencion.join(2000);
        }
    }

    @Test
    void transporte_jakarta_real_con_el_puerto_cerrado_da_down() throws Exception {
        final int puertoLibre;
        try (ServerSocket temporal = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puertoLibre = temporal.getLocalPort();
        }
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(puertoLibre);
        sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "1000");

        final Health salud = new SmtpHealthIndicator(new JakartaMailCorreoTransport(sender), "127.0.0.1", puertoLibre).health();

        assertEquals(Status.DOWN, salud.getStatus());
    }

    private static CorreoTransport transporte(final Verificacion verificacion) {
        return new CorreoTransport() {
            @Override
            public void enviar(final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp.CorreoSaliente correo) {
                throw new AssertionError("la salud nunca envia correos");
            }

            @Override
            public void verificarConexion() throws Exception {
                verificacion.ejecutar();
            }
        };
    }

    /** Dialogo SMTP minimo (saludo, EHLO, QUIT) para que JavaMail complete testConnection. */
    private static void atenderSmtpMinimo(final ServerSocket servidor) {
        try (Socket cliente = servidor.accept();
             BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.US_ASCII));
             OutputStream salida = cliente.getOutputStream()) {
            responder(salida, "220 prueba ESMTP");
            String linea;
            while ((linea = entrada.readLine()) != null) {
                final String comando = linea.toUpperCase();
                if (comando.startsWith("EHLO") || comando.startsWith("HELO")) {
                    responder(salida, "250 prueba");
                } else if (comando.startsWith("QUIT")) {
                    responder(salida, "221 adios");
                    return;
                } else {
                    responder(salida, "250 ok");
                }
            }
        } catch (final Exception _) {
            // el cliente puede cerrar antes de QUIT; la prueba solo mira el estado de salud
        }
    }

    private static void responder(final OutputStream salida, final String linea) throws java.io.IOException {
        salida.write((linea + "\r\n").getBytes(StandardCharsets.US_ASCII));
        salida.flush();
    }

    @FunctionalInterface
    private interface Verificacion {
        void ejecutar() throws Exception;
    }
}
