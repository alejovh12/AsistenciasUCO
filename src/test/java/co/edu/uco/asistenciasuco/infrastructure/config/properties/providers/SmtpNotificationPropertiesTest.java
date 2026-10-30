package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpNotificationPropertiesTest {

    @Test
    @DisplayName("BE-P01: defaults del CONTRACT §3.1 cuando se omiten opcionales")
    void BE_P01_defaults_del_contrato() {
        final SmtpNotificationProperties props = new SmtpNotificationProperties(
                "smtp.uco.edu.co",
                null,
                null,
                null,
                null,
                null,
                "notificaciones@uco.edu.co",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertEquals("smtp.uco.edu.co", props.host());
        assertEquals(587, props.port());
        assertEquals("", props.username());
        assertEquals("", props.password());
        assertTrue(props.starttls());
        assertFalse(props.ssl());
        assertEquals("notificaciones@uco.edu.co", props.fromAddress());
        assertEquals("Asistencia UCO", props.fromName());
        assertEquals(Duration.ofSeconds(10), props.connectTimeout());
        assertEquals(Duration.ofSeconds(20), props.readTimeout());
        assertEquals(50, props.maxBcc());
        assertEquals(60, props.ratePerMinute());
        assertEquals(500, props.ratePerDay());
        assertFalse(props.requireConfirmedEmail());
        assertEquals("", props.platformUrl());
        assertEquals(Duration.ofSeconds(60), props.templateCacheTtl());

        final SmtpNotificationProperties props15 = new SmtpNotificationProperties(
                "smtp.uco.edu.co",
                2525,
                "user",
                "secret",
                false,
                true,
                "avisos@uco.edu.co",
                "UCO Avisos",
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                25,
                30,
                300,
                true,
                "https://asistencia.uco.edu.co"
        );
        assertEquals(2525, props15.port());
        assertTrue(props15.ssl());
        assertFalse(props15.starttls());
        assertEquals(Duration.ofSeconds(60), props15.templateCacheTtl());
    }

    @Test
    @DisplayName("BE-P02: rechaza host vacio, puerto invalido, from-address invalido, starttls+ssl, rate-per-day < rate-per-minute y max-bcc fuera de 1..100")
    void BE_P02_rechaza_configuraciones_invalidas() {
        // Host nulo o vacio
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                null, 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "   ", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));

        // Puerto fuera de 1..65535
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 0, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 65536, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));

        // from-address invalido o con salto de linea
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "correo-sin-arroba", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "bad\r\n@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));

        // starttls y ssl simultaneos
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, true, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));

        // max-bcc fuera de 1..100
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 0, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 101, 60, 500, false, "", Duration.ofSeconds(60)
        ));

        // rate-per-minute fuera de 1..10000
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 0, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 10001, 20000, false, "", Duration.ofSeconds(60)
        ));

        // rate-per-day < rate-per-minute
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 59, false, "", Duration.ofSeconds(60)
        ));

        // connect-timeout / read-timeout / template-cache-ttl invalidos (<= 0)
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ZERO, Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ZERO, 50, 60, 500, false, "", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "", Duration.ofSeconds(-1)
        ));
    }

    @Test
    @DisplayName("BE-P03: la contrasena nunca aparece en toString()")
    void BE_P03_password_no_aparece_en_toString() {
        final String secret = "SuperClaveSecretaInstitucional#2026";
        final SmtpNotificationProperties props = new SmtpNotificationProperties(
                "smtp.uco.edu.co",
                587,
                "smtp-user",
                secret,
                true,
                false,
                "notificaciones@uco.edu.co",
                "Asistencia UCO",
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                50,
                60,
                500,
                false,
                "https://asistencia.uco.edu.co",
                Duration.ofSeconds(60)
        );

        final String repr = props.toString();
        assertFalse(repr.contains(secret));
        assertTrue(repr.contains("smtp.uco.edu.co"));
        assertTrue(repr.contains("[PROTECTED]"));
    }

    @Test
    @DisplayName("BE-P04: platformUrl vacia, http:// o https:// son aceptadas; javascript:, ftp: o texto libre se rechazan fail-fast")
    void BE_P04_platformUrl_valida_esquema_http_https_o_vacio() {
        final SmtpNotificationProperties vacia = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "   ", Duration.ofSeconds(60)
        );
        assertEquals("", vacia.platformUrl());

        final SmtpNotificationProperties conHttp = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "http://localhost:4200", Duration.ofSeconds(60)
        );
        assertEquals("http://localhost:4200", conHttp.platformUrl());

        final SmtpNotificationProperties conHttps = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "https://asistencia.uco.edu.co", Duration.ofSeconds(60)
        );
        assertEquals("https://asistencia.uco.edu.co", conHttps.platformUrl());

        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "javascript:alert(1)", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "ftp://archivos.uco.edu.co", Duration.ofSeconds(60)
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "asistencia.uco.edu.co", Duration.ofSeconds(60)
        ));
    }
}
