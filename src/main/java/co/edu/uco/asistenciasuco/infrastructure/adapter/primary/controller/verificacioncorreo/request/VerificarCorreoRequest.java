package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.request;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/v1/public/verificar-correo}: el token del fragmento del enlace y, IDENT-002, la contrasena
 * que define el titular al activar una cuenta registrada por otra persona (opcional; se ignora si la cuenta ya tiene una).
 */
public class VerificarCorreoRequest {

    @Size(max = 256, message = "El enlace de verificación no es válido.")
    private String token;

    @Size(max = 255, message = "La contraseña no puede superar 255 caracteres.")
    private String password;

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    /** Nunca se imprime el token: el valor es una credencial de un solo uso. */
    @Override
    public String toString() {
        return "VerificarCorreoRequest[token=***, password=***]";
    }
}
