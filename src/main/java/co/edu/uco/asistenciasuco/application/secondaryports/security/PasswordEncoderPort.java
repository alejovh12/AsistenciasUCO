package co.edu.uco.asistenciasuco.application.secondaryports.security;

/**
 * Puerto de aplicacion para codificar credenciales de usuario.
 * Dependencia transitoria del flujo legacy mientras las credenciales migran al proveedor de
 * identidad externo.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
