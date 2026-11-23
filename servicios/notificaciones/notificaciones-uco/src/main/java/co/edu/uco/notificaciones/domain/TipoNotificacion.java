package co.edu.uco.notificaciones.domain;

/**
 * Tipo de notificacion que el servicio sabe entregar. Los tipos {@code interno} (correo de prueba generico) no se
 * ofrecen para administrar plantillas ni se aceptan desde las aplicaciones cliente.
 */
public record TipoNotificacion(String tipo, String descripcion, boolean interno) {
}
