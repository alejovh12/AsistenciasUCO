package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OBS-07 (VAL-oleada8): metricas de negocio {@code asistencias.negocio.eventos{tipo}}. Se cuentan al publicar el evento de
 * dominio, haya o no clientes SSE conectados (el hecho de negocio ya ocurrio). Solo los tipos de la lista cerrada llevan su
 * etiqueta, para que la cardinalidad no crezca con eventos tecnicos.
 */
class MetricasNegocioRealtimeTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ReactorRealtimeAdapter adapter = new ReactorRealtimeAdapter(registry);

    private double eventos(final String tipo) {
        final Counter contador = registry.find(ReactorRealtimeAdapter.METRICA_EVENTOS_NEGOCIO).tag("tipo", tipo).counter();
        return contador == null ? 0 : contador.count();
    }

    @Test
    void cuenta_cada_evento_de_negocio_aunque_no_haya_clientes_conectados() {
        adapter.publish(RealtimeEvent.of("TOMA_ABIERTA", Map.of()));
        adapter.publish(RealtimeEvent.of("TOMA_ABIERTA", Map.of()));
        adapter.publish(RealtimeEvent.of("ASISTENCIA_QR_REGISTRADA", Map.of()));
        adapter.publish(RealtimeEvent.of("RECLAMO_ASISTENCIA_RADICADO", Map.of()));

        assertEquals(2, eventos("TOMA_ABIERTA"));
        assertEquals(1, eventos("ASISTENCIA_QR_REGISTRADA"));
        assertEquals(1, eventos("RECLAMO_ASISTENCIA_RADICADO"));
        assertEquals(4, registry.find("realtime.events.dropped").counter().count(), "sin clientes, el SSE los descarta");
    }

    @Test
    void los_eventos_tecnicos_no_crean_series_de_negocio() {
        adapter.publish(RealtimeEvent.of("ASISTENCIA_QR_ROTADO", Map.of()));
        adapter.publish(RealtimeEvent.of("PARAMETER_UPDATED", Map.of()));
        adapter.publish(null);

        assertNull(registry.find(ReactorRealtimeAdapter.METRICA_EVENTOS_NEGOCIO).counter());
    }

    @Test
    void la_lista_de_tipos_de_negocio_cubre_tomas_qr_reclamos_sesiones_y_alertas() {
        assertTrue(ReactorRealtimeAdapter.TIPOS_NEGOCIO.containsAll(java.util.Set.of(
                "TOMA_ABIERTA", "TOMA_CERRADA", "ASISTENCIA_QR_REGISTRADA", "RECLAMO_ASISTENCIA_RADICADO",
                "RECLAMO_ASISTENCIA_RESUELTO", "SESION_CANCELADA", "ALERTA_RIESGO_INASISTENCIA", "ALERTA_PERDIDA_INASISTENCIA")));
    }
}
