package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.NotificacionesServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacionesServiceHealthIndicatorTest {

    @Test
    void es_up_cuando_el_readiness_del_servicio_responde_up() {
        final NotificacionesServiceClient cliente = mock(NotificacionesServiceClient.class);
        when(cliente.readiness()).thenReturn(true);

        assertEquals(Status.UP, new NotificacionesServiceHealthIndicator(cliente).health().getStatus());
    }

    @Test
    void es_down_sin_exponer_detalles_del_servicio_cuando_no_responde() {
        final NotificacionesServiceClient cliente = mock(NotificacionesServiceClient.class);
        when(cliente.readiness()).thenReturn(false);

        final var salud = new NotificacionesServiceHealthIndicator(cliente).health();

        assertEquals(Status.DOWN, salud.getStatus());
        assertEquals(1, salud.getDetails().size());
    }

    @Test
    void el_cliente_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new NotificacionesServiceHealthIndicator(null));
    }
}
