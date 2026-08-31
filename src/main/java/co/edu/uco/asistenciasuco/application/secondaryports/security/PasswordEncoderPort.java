package co.edu.uco.asistenciasuco.application.secondaryports.security;

/**
 * Puerto de aplicacion para codificar credenciales de usuario.
 * Dependencia transitoria del flujo legacy mientras credenciales migra completamente a Keycloak.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
