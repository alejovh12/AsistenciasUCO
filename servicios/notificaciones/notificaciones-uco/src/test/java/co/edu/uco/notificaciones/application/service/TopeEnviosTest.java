package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.RelojFijo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopeEnviosTest {

    private final RelojFijo reloj = new RelojFijo(Instant.parse("2026-11-20T10:00:10Z"));
    private final MetricasFake metricas = new MetricasFake();

    @Test
    void agotaLaVentanaDelMinutoYSeRenueva() {
        final TopeEnvios tope = new TopeEnvios(2, 10, reloj, metricas);
        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());
        assertTrue(tope.intentarConsumir());
        assertFalse(tope.disponible());
        assertFalse(tope.intentarConsumir());
        assertTrue(metricas.contiene("window,minute"));
        reloj.avanzar(Duration.ofMinutes(1));
        assertTrue(tope.disponible());
        assertTrue(tope.intentarConsumir());
    }

    @Test
    void agotaLaVentanaDelDiaYSeRenueva() {
        final TopeEnvios tope = new TopeEnvios(2, 2, reloj, metricas);
        assertTrue(tope.intentarConsumir());
        assertTrue(tope.intentarConsumir());
        reloj.avanzar(Duration.ofMinutes(2));
        assertFalse(tope.intentarConsumir());
        assertTrue(metricas.contiene("window,day"));
        reloj.avanzar(Duration.ofDays(1));
        assertTrue(tope.intentarConsumir());
    }

    @Test
    void reiniciarVentanasDevuelveElCupo() {
        final TopeEnvios tope = new TopeEnvios(1, 5, reloj, metricas);
        assertTrue(tope.intentarConsumir());
        assertFalse(tope.disponible());
        tope.reiniciarVentanas();
        assertTrue(tope.disponible());
    }

    @Test
    void validaLosLimites() {
        assertThrows(IllegalArgumentException.class, () -> new TopeEnvios(0, 5, reloj, metricas));
        assertThrows(IllegalArgumentException.class, () -> new TopeEnvios(5, 4, reloj, metricas));
        assertThrows(NullPointerException.class, () -> new TopeEnvios(1, 5, null, metricas));
        assertThrows(NullPointerException.class, () -> new TopeEnvios(1, 5, reloj, null));
    }
}
