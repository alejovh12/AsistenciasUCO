package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResumenCola;

import java.util.List;

/** Misma forma que {@code ApiPageResponse} de Asistencias, mas el resumen de la cola. */
public record PaginaNotificacionesResponse(
        List<NotificacionAdminResponse> elementos,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        ResumenCola resumen
) {

    public static PaginaNotificacionesResponse desde(final PaginaNotificaciones p) {
        return new PaginaNotificacionesResponse(p.elementos().stream().map(NotificacionAdminResponse::desde).toList(),
                p.numeroPagina(), p.tamanoPagina(), p.totalElementos(), p.totalPaginas(), p.resumen());
    }
}
