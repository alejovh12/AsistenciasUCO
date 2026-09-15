package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Puerta de entrada tecnica que expone el canal realtime a los controllers, sin que estos
 * necesiten depender de {@code application.secondaryports.realtime} ni de
 * {@code infrastructure.adapter.secondary} (ver ArchUnit
 * {@code ControllersMustDependOnlyOnInputPortsTest}). Mismo patron que
 * {@code infrastructure.adapter.primary.security.AuthenticatedUserResolver}.
 */
public interface RealtimeStreamGateway {

    Flux<RealtimeEventResponse> subscribe();

    int activeSubscribersCount();

    /**
     * Utilidad de desarrollo (ver {@code POST /api/v1/realtime/emit}, restringido a
     * ADMINISTRADOR). No es el mecanismo principal de publicacion: los flujos de negocio reales
     * publican via {@code RealtimePublisherPort} desde Application.
     */
    void emit(String type, Map<String, Object> payload);
}
