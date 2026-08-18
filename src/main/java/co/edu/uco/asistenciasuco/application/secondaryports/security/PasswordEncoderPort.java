package co.edu.uco.asistenciasuco.application.secondaryports.security;

/**
 * Puerto de aplicacion para codificar credenciales de usuario.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
