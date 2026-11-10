package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

/**
 * Respuesta con datos y el {@code mensajeUsuario} que entrego la DB (catalogo de mensajes), listo para presentar (DEC-065 (2): aprobar y
 * rechazar solicitudes de sesion). El front lo muestra tal cual.
 */
public record ApiDataMensajeResponse<T>(
        boolean exitoso,
        T datos,
        String mensajeUsuario
) {
}
