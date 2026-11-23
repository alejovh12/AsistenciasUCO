package co.edu.uco.notificaciones.application.model;

/** Filtros opcionales de la cola administrativa (null = sin filtro). */
public record FiltroNotificaciones(String estado, String tipo, String aplicacion) {
}
