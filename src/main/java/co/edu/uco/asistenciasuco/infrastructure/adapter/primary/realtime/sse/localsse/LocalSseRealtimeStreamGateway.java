package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

/**
 * Bridge interno entre el controller SSE y el adapter local seleccionado por el Composition Root.
 *
 * <p>No se autoregistra con Spring. {@code LocalSseRealtimeAdapterConfiguration} crea esta
 * implementacion solamente cuando el provider realtime es {@code local-sse}.</p>
 */
public final class LocalSseRealtimeStreamGateway implements RealtimeStreamGateway {

    private final ReactorRealtimeAdapter realtimeAdapter;

    public LocalSseRealtimeStreamGateway(final ReactorRealtimeAdapter realtimeAdapter) {
        this.realtimeAdapter = Objects.requireNonNull(realtimeAdapter, "ReactorRealtimeAdapter es requerido.");
    }

    @Override
    public Flux<RealtimeEventResponse> subscribe() {
        return realtimeAdapter.events().map(RealtimeEventResponse::from);
    }

    @Override
    public int activeSubscribersCount() {
        return realtimeAdapter.activeSubscribersCount();
    }

    @Override
    public void emit(final String type, final Map<String, Object> payload) {
        realtimeAdapter.publish(RealtimeEvent.of(type, payload));
    }
}
