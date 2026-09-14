package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.ReactorRealtimeAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

@Component
final class RealtimeStreamGatewayImpl implements RealtimeStreamGateway {

    private final ReactorRealtimeAdapter realtimeAdapter;

    RealtimeStreamGatewayImpl(final ReactorRealtimeAdapter realtimeAdapter) {
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
