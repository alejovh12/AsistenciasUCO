package co.edu.uco.notificaciones.infrastructure.config;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionFactoryTest {

    private static NotificacionesProperties.Smtp smtp(final String host, final String from, final int port, final boolean starttls, final boolean ssl,
                                                      final int maxBcc, final int porMinuto, final int porDia, final String url,
                                                      final boolean modoPrueba, final String redirecciones) {
        return new NotificacionesProperties.Smtp(host, port, "usuario", "clave", starttls, ssl, from, "UCO", Duration.ofSeconds(10),
                Duration.ofSeconds(20), maxBcc, porMinuto, porDia, url, new NotificacionesProperties.TestMode(modoPrueba, redirecciones));
    }

    private static NotificacionesProperties.Smtp smtpValido() {
        return smtp("smtp.local", "noreply@uco.edu.co", 587, true, false, 50, 60, 500, "http://localhost:4200", false, "");
    }

    private static NotificacionesProperties props(final String canal, final NotificacionesProperties.Smtp smtp) {
        return new NotificacionesProperties("k", canal, smtp, dispatcherValido());
    }

    private static NotificacionesProperties.Dispatcher dispatcherValido() {
        return new NotificacionesProperties.Dispatcher(true, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5, Duration.ofSeconds(30),
                Duration.ofMinutes(15), 30, Duration.ofHours(6), "inst-1");
    }

    private static void rechaza(final NotificacionesProperties p, final String fragmento) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfiguracionFactory.canal(p));
        assertTrue(ex.getMessage().contains(fragmento), ex.getMessage());
    }

    @Test
    void canalSmtpValidoNoExponeLaClave() {
        final ConfiguracionCanal c = ConfiguracionFactory.canal(props(" SMTP ", smtpValido()));
        assertEquals("smtp", c.canal());
        assertEquals("smtp.local", c.host());
        assertTrue(c.tienePassword());
        assertEquals("noreply@uco.edu.co", c.fromAddress());
        assertFalse(c.testModeEnabled());
        assertTrue(c.testModeRedirectTo().isEmpty());
    }

    @Test
    void canalLogNoExigeSmtpYUsaUnRemitenteDeRespaldo() {
        final ConfiguracionCanal c = ConfiguracionFactory.canal(props("log", smtp(null, "", 587, true, false, 50, 60, 500, null, false, "")));
        assertEquals("log", c.canal());
        assertEquals("", c.host());
        assertEquals("notificaciones@localhost.localdomain", c.fromAddress());
        assertEquals("", c.platformUrl());
    }

    @Test
    void validaElCanalYLosLimites() {
        rechaza(props("correo", smtpValido()), "notificaciones.canal");
        rechaza(props(null, smtpValido()), "notificaciones.canal");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, true, 50, 60, 500, "", false, "")), "STARTTLS");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 0, true, false, 50, 60, 500, "", false, "")), "PORT");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 70000, true, false, 50, 60, 500, "", false, "")), "PORT");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 0, 60, 500, "", false, "")), "MAX_BCC");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 101, 60, 500, "", false, "")), "MAX_BCC");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 0, 500, "", false, "")), "RATE_PER_MINUTE");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 10001, 20000, "", false, "")), "RATE_PER_MINUTE");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 59, "", false, "")), "RATE_PER_DAY");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "ftp://x", false, "")), "PLATFORM_URL");
        rechaza(props("smtp", smtp(" ", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", false, "")), "HOST");
        rechaza(props("smtp", smtp("h", "malo", 587, true, false, 50, 60, 500, "", false, "")), "FROM_ADDRESS");
        rechaza(props("log", smtp("h", "malo", 587, true, false, 50, 60, 500, "", false, "")), "FROM_ADDRESS");
    }

    @Test
    void modoPruebaExigeUnoACincoCorreosValidos() {
        final ConfiguracionCanal c = ConfiguracionFactory.canal(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", true,
                " p@uco.edu.co , q@uco.edu.co,p@uco.edu.co")));
        assertEquals(java.util.List.of("p@uco.edu.co", "q@uco.edu.co"), c.testModeRedirectTo());
        assertTrue(c.testModeEnabled());
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", true, "")), "TEST_MODE_REDIRECT_TO");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", true, null)), "TEST_MODE_REDIRECT_TO");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", true, "malo")), "TEST_MODE_REDIRECT_TO");
        rechaza(props("smtp", smtp("h", "a@uco.edu.co", 587, true, false, 50, 60, 500, "", true, "a@uco.edu.co,b@uco.edu.co,c@uco.edu.co,d@uco.edu.co,e@uco.edu.co,f@uco.edu.co")),
                "TEST_MODE_REDIRECT_TO");
    }

    private static void rechazaDespachador(final UnaryOperator<NotificacionesProperties.Dispatcher> cambio, final String fragmento) {
        final NotificacionesProperties.Dispatcher d = cambio.apply(dispatcherValido());
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfiguracionFactory.despachador(d));
        assertTrue(ex.getMessage().contains(fragmento), ex.getMessage());
    }

    private static NotificacionesProperties.Dispatcher con(final NotificacionesProperties.Dispatcher d, final int lote, final Duration lease,
                                                           final Duration barrido, final int intentos, final Duration base, final Duration max,
                                                           final int dias, final Duration retencion, final String instancia) {
        return new NotificacionesProperties.Dispatcher(d.enabled(), lote, lease, barrido, intentos, base, max, dias, retencion, instancia);
    }

    @Test
    void despachadorValidoUsaLosValoresYGeneraInstancia() {
        final ConfiguracionDespachador d = ConfiguracionFactory.despachador(dispatcherValido());
        assertEquals("inst-1", d.instanceId());
        assertEquals(Duration.ofHours(6), d.retentionInterval());
        final ConfiguracionDespachador sinInstancia = ConfiguracionFactory.despachador(
                con(dispatcherValido(), 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5, Duration.ofSeconds(30), Duration.ofMinutes(15), 30, null, " "));
        assertTrue(sinInstancia.instanceId().contains("-"));
        assertEquals(Duration.ofHours(6), sinInstancia.retentionInterval());
        assertFalse(ConfiguracionFactory.generarInstancia().isBlank());
    }

    @Test
    void despachadorValidaLosLimites() {
        rechazaDespachador(d -> con(d, 0, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "BATCH_SIZE");
        rechazaDespachador(d -> con(d, 501, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "BATCH_SIZE");
        rechazaDespachador(d -> con(d, 5, null, d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "LEASE");
        rechazaDespachador(d -> con(d, 5, Duration.ofSeconds(4), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "LEASE");
        rechazaDespachador(d -> con(d, 5, Duration.ofHours(2), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "LEASE");
        rechazaDespachador(d -> con(d, 5, d.lease(), null, 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "SWEEPER");
        rechazaDespachador(d -> con(d, 5, d.lease(), Duration.ofMillis(10), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "SWEEPER");
        rechazaDespachador(d -> con(d, 5, d.lease(), Duration.ofHours(2), 5, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "SWEEPER");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 0, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "MAX_ATTEMPTS");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 21, d.backoffBase(), d.backoffMax(), 30, d.retentionInterval(), "i"), "MAX_ATTEMPTS");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, null, d.backoffMax(), 30, d.retentionInterval(), "i"), "BACKOFF_BASE");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, Duration.ofMillis(500), d.backoffMax(), 30, d.retentionInterval(), "i"), "BACKOFF_BASE");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, Duration.ofHours(2), d.backoffMax(), 30, d.retentionInterval(), "i"), "BACKOFF_BASE");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), null, 30, d.retentionInterval(), "i"), "BACKOFF_MAX");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), Duration.ofSeconds(1), 30, d.retentionInterval(), "i"), "BACKOFF_MAX");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), Duration.ofDays(2), 30, d.retentionInterval(), "i"), "BACKOFF_MAX");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 0, d.retentionInterval(), "i"), "RETENTION_DAYS");
        rechazaDespachador(d -> con(d, 5, d.lease(), d.sweeperInterval(), 5, d.backoffBase(), d.backoffMax(), 4000, d.retentionInterval(), "i"), "RETENTION_DAYS");
    }
}
