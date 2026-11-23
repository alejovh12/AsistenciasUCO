package co.edu.uco.notificaciones.application.model;

import co.edu.uco.notificaciones.domain.Notificacion;

import java.util.List;

public record PaginaNotificaciones(
        List<Notificacion> elementos,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        ResumenCola resumen
) {

    public PaginaNotificaciones {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}
