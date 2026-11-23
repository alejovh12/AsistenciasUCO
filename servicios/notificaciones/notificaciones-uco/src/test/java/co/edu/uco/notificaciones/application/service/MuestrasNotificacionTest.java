package co.edu.uco.notificaciones.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuestrasNotificacionTest {

    @Test
    void valorParaMarcadorConocidoYDesconocido() {
        assertEquals("15.5", MuestrasNotificacion.valorPara("porcentaje"));
        assertEquals("[inventado]", MuestrasNotificacion.valorPara("inventado"));
    }

    @Test
    void completoYBasicoSonCopiasIndependientes() {
        final var completo = MuestrasNotificacion.completo();
        assertTrue(completo.containsKey("enlace"));
        completo.clear();
        assertFalse(MuestrasNotificacion.completo().isEmpty());
        assertEquals(8, MuestrasNotificacion.basicoVistaPrevia().size());
    }
}
