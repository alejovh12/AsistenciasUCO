package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
        final Duration d10 = Duration.ofSeconds(10);
        final Duration d20 = Duration.ofSeconds(20);
        final Duration d60 = Duration.ofSeconds(60);
        final Duration dNeg1 = Duration.ofSeconds(-1);

        // Host nulo o vacio
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                null, 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "   ", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));

        // Puerto fuera de 1..65535
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 0, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 65536, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));

        // from-address invalido o con salto de linea
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "correo-sin-arroba", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "bad\r\n@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));

        // starttls y ssl simultaneos
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, true, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", d60
        ));

        // max-bcc fuera de 1..100
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 0, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 101, 60, 500, false, "", d60
        ));

        // rate-per-minute fuera de 1..10000
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 0, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 10001, 20000, false, "", d60
        ));

        // rate-per-day < rate-per-minute
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 59, false, "", d60
        ));

        // connect-timeout / read-timeout / template-cache-ttl invalidos (<= 0)
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ZERO, d20, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, Duration.ZERO, 50, 60, 500, false, "", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "", dNeg1
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

        final Duration d10 = Duration.ofSeconds(10);
        final Duration d20 = Duration.ofSeconds(20);
        final Duration d60 = Duration.ofSeconds(60);

        final SmtpNotificationProperties conHttps = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "https://asistencia.uco.edu.co", d60
        );
        assertEquals("https://asistencia.uco.edu.co", conHttps.platformUrl());

        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "javascript:alert(1)", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "ftp://archivos.uco.edu.co", d60
        ));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                d10, d20, 50, 60, 500, false, "asistencia.uco.edu.co", d60
        ));
    }

    @Test
    @DisplayName("BE-P05: defaults de test-mode: enabled=false y redirect-to vacio")
    void BE_P05_defaults_test_mode() {
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
                null,
                null
        );

        assertFalse(props.testMode().enabled());
        assertTrue(props.testMode().redirectTo().isEmpty());
    }

    @Test
    @DisplayName("BE-P06: test-mode.enabled=true con redirect-to vacio, direccion insegura o mas de 5 direcciones -> IllegalArgumentException (fail-fast)")
    void BE_P06_test_mode_activo_rechaza_redirect_to_vacio_inseguro_o_mas_de_5() {
        // Vacio o nulo con enabled=true
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, ""));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "   "));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, (String) null));

        // Direccion insegura: a@b.co,c.com / <a@b.co> / con espacios / con CR/LF / punto y coma / comillas / parentesis
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "a@b.co,c.com"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "<a@b.co>"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "a b@b.co"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "a@b.co\r\nBcc:x@y.co"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "a@b.co;x@y.co"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "\"a\"@b.co"));
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(true, "a(1)@b.co"));

        // Mas de 5 direcciones distintas
        assertThrows(IllegalArgumentException.class, () -> new SmtpNotificationProperties.TestMode(
                true,
                "u1@x.co, u2@x.co, u3@x.co, u4@x.co, u5@x.co, u6@x.co"
        ));
    }

    @Test
    @DisplayName("BE-P07: redirect-to con 'a@x.co, b@x.co ,a@x.co' -> 2 direcciones recortadas y sin duplicados; con enabled=false el valor se ignora sin error")
    void BE_P07_test_mode_normaliza_y_deduplica_o_ignora_cuando_apagado() {
        final SmtpNotificationProperties.TestMode modoActivo = new SmtpNotificationProperties.TestMode(
                true,
                "a@x.co, b@x.co ,a@x.co"
        );
        assertTrue(modoActivo.enabled());
        assertEquals(List.of("a@x.co", "b@x.co"), modoActivo.redirectTo());

        final SmtpNotificationProperties propsActivas = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false, "no-reply@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20), 50, 60, 500, false, "https://asistencia.uco.edu.co",
                Duration.ofSeconds(60), modoActivo
        );
        assertTrue(propsActivas.testMode().enabled());
        assertEquals(List.of("a@x.co", "b@x.co"), propsActivas.testMode().redirectTo());

        // Con enabled=false (o null -> false) el valor de redirect-to se ignora sin error aunque sea vacio o invalido
        final SmtpNotificationProperties.TestMode modoApagado = new SmtpNotificationProperties.TestMode(
                false,
                "direccion-invalida-ignorada, <otra@mala>"
        );
        assertFalse(modoApagado.enabled());
        assertTrue(modoApagado.redirectTo().isEmpty());

        final SmtpNotificationProperties.TestMode modoNull = new SmtpNotificationProperties.TestMode(
                null,
                "a@x.co, b@x.co"
        );
        assertFalse(modoNull.enabled());
        assertTrue(modoNull.redirectTo().isEmpty());
    }
}
