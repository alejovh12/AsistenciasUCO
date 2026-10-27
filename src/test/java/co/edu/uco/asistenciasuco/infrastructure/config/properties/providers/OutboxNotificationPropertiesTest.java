package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties.Sender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxNotificationPropertiesTest {

    @Test
    @DisplayName("BE-P02: propiedades con valores validos")
    void BE_P02_valores_validos() {
        final OutboxNotificationProperties props = new OutboxNotificationProperties(
                Sender.LOG,
                true,
                50,
                Duration.ofSeconds(60),
                Duration.ofSeconds(30),
                5,
                Duration.ofSeconds(30),
                Duration.ofMinutes(15),
                30,
                Duration.ofHours(6),
                "instancia-1"
        );

        assertEquals(50, props.batchSize());
        assertEquals(5, props.maxAttempts());
        assertEquals("instancia-1", props.instanceId());
    }

    @Test
    @DisplayName("BE-P02: batchSize fuera de rango (1..500) lanza excepcion")
    void BE_P02_batch_size_fuera_de_rango() {
        final IllegalArgumentException exMin = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 0, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMin.getMessage().contains("batch-size"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 501, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMax.getMessage().contains("batch-size"));
    }

    @Test
    @DisplayName("BE-P02: lease fuera de rango (5s..1h) lanza excepcion")
    void BE_P02_lease_fuera_de_rango() {
        final IllegalArgumentException exMin = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, Duration.ofSeconds(4), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMin.getMessage().contains("lease"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, Duration.ofHours(2), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 30, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMax.getMessage().contains("lease"));
    }

    @Test
    @DisplayName("BE-P02: backoffMax menor a backoffBase lanza excepcion")
    void BE_P02_backoff_max_menor_a_base() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                        Duration.ofMinutes(10), Duration.ofSeconds(30), 30, Duration.ofHours(6), ""
                )
        );
        assertTrue(ex.getMessage().contains("backoff-max"));
    }

    @Test
    @DisplayName("BE-P02: retentionDays fuera de rango (1..3650) lanza excepcion")
    void BE_P02_retention_days_fuera_de_rango() {
        final IllegalArgumentException exMin = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 0, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMin.getMessage().contains("retention-days"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                        Duration.ofSeconds(30), Duration.ofMinutes(15), 3651, Duration.ofHours(6), ""
                )
        );
        assertTrue(exMax.getMessage().contains("retention-days"));
    }
}
