package co.edu.uco.notificaciones.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Aviso en la cola de envio del servicio. */
public record Notificacion(
        UUID id,
        String aplicacion,
        String idempotencyKey,
        String tipo,
        String idioma,
        String asunto,
        String mensaje,
        List<Destinatario> destinatarios,
        Map<String, String> datos,
        EstadoEnvio estado,
        int intentos,
        Instant creadoEn,
        Instant proximoIntentoEn,
        Instant reservadoHasta,
        String reservadoPor,
        Instant entregadoEn,
        Instant falloEn,
        String codigoError,
        String detalleError,
        String idCorrelacion,
        String traceparent
) {

    public Notificacion {
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }
}
