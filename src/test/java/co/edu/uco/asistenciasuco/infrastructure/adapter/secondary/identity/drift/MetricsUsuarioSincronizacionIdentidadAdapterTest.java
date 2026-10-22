package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.drift;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * IAM-001 §3 (parte minima consumida por HU121/HU122): el desfase DB-IdP deja traza estructurada y la metrica
 * {@code identity_drift_total{operation}}. La persistencia en tabla y la reconciliacion siguen siendo de IAM-001.
 */
class MetricsUsuarioSincronizacionIdentidadAdapterTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricsUsuarioSincronizacionIdentidadAdapter adapter = new MetricsUsuarioSincronizacionIdentidadAdapter(registry);

    private double contador(final String operacion) {
        return registry.counter("identity_drift_total", "operation", operacion).count();
    }

    @Test
    void registrar_incrementa_el_contador_de_la_operacion() {
        adapter.registrar(UUID.randomUUID(), "DESACTIVAR", "HTTP 500", UUID.randomUUID());
        adapter.registrar(UUID.randomUUID(), "DESACTIVAR", "HTTP 503", UUID.randomUUID());
        adapter.registrar(UUID.randomUUID(), "ACTUALIZAR", "conflicto de correo", UUID.randomUUID());

        assertEquals(2.0, contador("DESACTIVAR"));
        assertEquals(1.0, contador("ACTUALIZAR"));
        assertEquals(0.0, contador("REACTIVAR"));
    }

    @Test
    void registrar_tolera_valores_nulos_sin_lanzar() {
        assertDoesNotThrow(() -> adapter.registrar(null, "REACTIVAR", null, null));
        assertEquals(1.0, contador("REACTIVAR"));
    }
}
