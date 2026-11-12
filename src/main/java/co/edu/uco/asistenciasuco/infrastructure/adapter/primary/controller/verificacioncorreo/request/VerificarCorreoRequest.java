package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.request;

import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/public/verificar-correo}: el token del fragmento del enlace. */
public class VerificarCorreoRequest {

    @Size(max = 256, message = "El enlace de verificacion no es valido.")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    /** Nunca se imprime el token: el valor es una credencial de un solo uso. */
    @Override
    public String toString() {
        return "VerificarCorreoRequest[token=***]";
    }
}
