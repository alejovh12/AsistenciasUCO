package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Composite;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Provider;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationAdapterPropertiesTest {

    @Test
    @DisplayName("BE-P01: provider obligatorio")
    void BE_P01_provider_obligatorio() {
        final NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new NotificationAdapterProperties(null, null)
        );
        assertTrue(ex.getMessage().contains("app.adapters.notification.provider"));
    }

    @Test
    @DisplayName("BE-P01: composite con targets vacio o menos de 2 destinos lanza excepcion")
    void BE_P01_composite_menos_de_dos_destinos_falla() {
        final IllegalArgumentException exVacio = assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationAdapterProperties(Provider.COMPOSITE, new Composite(List.of()))
        );
        assertTrue(exVacio.getMessage().contains("app.adapters.notification.composite.targets"));

        final IllegalArgumentException exUno = assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationAdapterProperties(Provider.COMPOSITE, new Composite(List.of(Target.LOG)))
        );
        assertTrue(exUno.getMessage().contains("app.adapters.notification.composite.targets"));
    }

    @Test
    @DisplayName("BE-P01: composite con destinos duplicados lanza excepcion")
    void BE_P01_composite_destinos_duplicados_falla() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationAdapterProperties(Provider.COMPOSITE, new Composite(List.of(Target.LOG, Target.LOG)))
        );
        assertTrue(ex.getMessage().contains("app.adapters.notification.composite.targets"));
    }

    @Test
    @DisplayName("BE-P01: composite valido en cualquier orden")
    void BE_P01_composite_valido() {
        assertDoesNotThrow(() -> new NotificationAdapterProperties(
                Provider.COMPOSITE, new Composite(List.of(Target.LOG, Target.OUTBOX))
        ));

        assertDoesNotThrow(() -> new NotificationAdapterProperties(
                Provider.COMPOSITE, new Composite(List.of(Target.OUTBOX, Target.LOG))
        ));
    }

    @Test
    @DisplayName("BE-P01: provider LOG ignora targets nulo o vacio")
    void BE_P01_log_ignora_targets() {
        assertDoesNotThrow(() -> new NotificationAdapterProperties(Provider.LOG, null));
        assertDoesNotThrow(() -> new NotificationAdapterProperties(Provider.LOG, new Composite(List.of())));
    }
}
