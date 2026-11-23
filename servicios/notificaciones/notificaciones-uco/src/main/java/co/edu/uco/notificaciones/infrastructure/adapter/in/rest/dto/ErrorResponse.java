package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

/** Cuerpo uniforme de error: codigo estable, mensaje para mostrar y correlacion para rastrear en los logs. */
public record ErrorResponse(String code, String message, String correlationId) {
}
