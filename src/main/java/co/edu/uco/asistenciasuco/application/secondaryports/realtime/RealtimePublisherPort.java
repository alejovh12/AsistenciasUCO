package co.edu.uco.asistenciasuco.application.secondaryports.realtime;

/**
 * Puerto de salida neutral para publicar eventos de negocio en tiempo real.
 *
 * <p>Expresa unicamente la intencion de publicar; no conoce Reactor, SSE, WebSocket ni ningun
 * otro detalle de transporte. La implementacion vive en infraestructura.</p>
 *
 * <p>Contrato de resiliencia: realtime es un mecanismo secundario. Un fallo al publicar (sin
 * suscriptores, backpressure, sink terminado, etc.) <strong>nunca</strong> debe propagarse como
 * excepcion hacia quien invoca {@link #publish(RealtimeEvent)} ni provocar un rollback falso
 * sobre una operacion de negocio ya persistida. Los adaptadores deben manejar internamente
 * cualquier falla de emision (log, metrica, descarte controlado).</p>
 */
public interface RealtimePublisherPort {

    void publish(RealtimeEvent event);
}
