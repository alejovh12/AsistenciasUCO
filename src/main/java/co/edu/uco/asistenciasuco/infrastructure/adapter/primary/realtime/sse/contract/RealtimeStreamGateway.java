package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * Puerta de entrada tecnica que expone el canal realtime a los controllers, sin que estos
 * necesiten depender de {@code application.secondaryports.realtime} ni de
 * {@code infrastructure.adapter.secondary} (ver ArchUnit
 * {@code ControllersMustDependOnlyOnInputPortsTest}). Mismo patron que
 * {@code infrastructure.adapter.primary.security.AuthenticatedUserResolver}.
 */
public interface RealtimeStreamGateway {

    /**
     * Suscribe al {@code usuarioId} autenticado al canal de eventos de negocio de un
     * {@code grupoId} especifico. La autorizacion (titularidad del docente sobre el grupo) se
     * valida UNA sola vez al establecer la suscripcion; el filtrado por grupo ocurre despues en
     * memoria sobre el {@link Flux}, sin volver a consultar la base de datos por cada evento.
     *
     * @throws co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException
     *         si el usuario no tiene acceso autorizado al grupo solicitado.
     */
    Flux<RealtimeEventResponse> subscribe(UUID usuarioId, UUID grupoId);

    int activeSubscribersCount();

    /**
     * Utilidad de desarrollo (ver {@code POST /api/v1/realtime/emit}, restringido a
     * ADMINISTRADOR). No es el mecanismo principal de publicacion: los flujos de negocio reales
     * publican via {@code RealtimePublisherPort} desde Application.
     */
    void emit(String type, Map<String, Object> payload);
}
