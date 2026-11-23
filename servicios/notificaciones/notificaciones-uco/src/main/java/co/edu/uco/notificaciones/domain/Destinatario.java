package co.edu.uco.notificaciones.domain;

/** Persona que recibe el correo. El correo ya viene resuelto por la aplicacion cliente. */
public record Destinatario(String correo, String nombre) {
}
