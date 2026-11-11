package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request;

/** Cuerpo de {@code POST /api/v1/public/recuperacion-clave/confirmacion}: el token del enlace y la contrasena nueva. */
public class RestablecerClaveRequest {

    private String token;
    private String clave;

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(final String clave) {
        this.clave = clave;
    }

    /** Nunca se imprimen el token ni la contrasena: los dos son credenciales. */
    @Override
    public String toString() {
        return "RestablecerClaveRequest[token=***, clave=***]";
    }
}
