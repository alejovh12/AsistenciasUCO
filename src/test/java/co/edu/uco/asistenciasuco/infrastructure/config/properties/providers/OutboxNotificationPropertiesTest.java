package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties.Sender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxNotificationPropertiesTest {

    private static final Duration SEC_4 = Duration.ofSeconds(4);
    private static final Duration SEC_30 = Duration.ofSeconds(30);
    private static final Duration SEC_60 = Duration.ofSeconds(60);
    private static final Duration MIN_10 = Duration.ofMinutes(10);
    private static final Duration MIN_15 = Duration.ofMinutes(15);
    private static final Duration HR_2 = Duration.ofHours(2);
    private static final Duration HR_5 = Duration.ofHours(5);
    private static final Duration HR_6 = Duration.ofHours(6);
    private static final Duration HR_25 = Duration.ofHours(25);
    private static final Duration MS_500 = Duration.ofMillis(500);

    @Test
    @DisplayName("BE-P02: propiedades con valores validos")
    void BE_P02_valores_validos() {
        final OutboxNotificationProperties props = new OutboxNotificationProperties(
                Sender.LOG,
                true,
                50,
                SEC_60,
                SEC_30,
                5,
                SEC_30,
                MIN_15,
                30,
                HR_6,
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
                        Sender.LOG, true, 0, SEC_60, SEC_30, 5,
                        SEC_30, MIN_15, 30, HR_6, ""
                )
        );
        assertTrue(exMin.getMessage().contains("batch-size"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 501, SEC_60, SEC_30, 5,
                        SEC_30, MIN_15, 30, HR_6, ""
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
                        Sender.LOG, true, 50, SEC_4, SEC_30, 5,
                        SEC_30, MIN_15, 30, HR_6, ""
                )
        );
        assertTrue(exMin.getMessage().contains("lease"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, HR_2, SEC_30, 5,
                        SEC_30, MIN_15, 30, HR_6, ""
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
                        Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                        MIN_10, SEC_30, 30, HR_6, ""
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
                        Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                        SEC_30, MIN_15, 0, HR_6, ""
                )
        );
        assertTrue(exMin.getMessage().contains("retention-days"));

        final IllegalArgumentException exMax = assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxNotificationProperties(
                        Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                        SEC_30, MIN_15, 3651, HR_6, ""
                )
        );
        assertTrue(exMax.getMessage().contains("retention-days"));
    }

    @Test
    @DisplayName("BE-P02: sweeperInterval fuera de rango (1s..1h) lanza excepcion")
    void BE_P02_sweeper_interval_fuera_de_rango() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, MS_500, 5,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, HR_2, 5,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
    }

    @Test
    @DisplayName("BE-P02: maxAttempts fuera de rango (1..20) lanza excepcion")
    void BE_P02_max_attempts_fuera_de_rango() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 0,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 21,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
    }

    @Test
    @DisplayName("BE-P02: backoffBase fuera de rango (1s..1h) lanza excepcion")
    void BE_P02_backoff_base_fuera_de_rango() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                MS_500, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                HR_2, HR_5, 30, HR_6, ""
        ));
    }

    @Test
    @DisplayName("BE-P02: backoffMax mayor a 24h lanza excepcion")
    void BE_P02_backoff_max_mayor_a_24h() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                SEC_30, HR_25, 30, HR_6, ""
        ));
    }

    @Test
    @DisplayName("BE-P02: duraciones nulas lanzan excepcion")
    void BE_P02_duraciones_nulas() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, null, SEC_30, 5,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, null, 5,
                SEC_30, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                null, MIN_15, 30, HR_6, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new OutboxNotificationProperties(
                Sender.LOG, true, 50, SEC_60, SEC_30, 5,
                SEC_30, null, 30, HR_6, ""
        ));
    }

    @Test
    @DisplayName("BE-P02: valores nulos en sender e instanceId se resuelven a defaults seguros")
    void BE_P02_defaults_seguros() {
        final OutboxNotificationProperties props = new OutboxNotificationProperties(
                null, true, 50, Duration.ofSeconds(60), Duration.ofSeconds(30), 5,
                Duration.ofSeconds(30), Duration.ofMinutes(15), 30, null, null
        );
        assertEquals(Sender.LOG, props.sender());
        assertEquals(Duration.ofHours(6), props.retentionInterval());
        assertNotNull(props.instanceId());
        assertFalse(props.instanceId().isBlank());
    }
}
