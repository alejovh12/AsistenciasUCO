package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request;

/** Cuenta cuyo estado de acceso se consulta o reconcilia, identificada por su correo institucional. */
public final class AccesoCuentaRequest {

    private String correo;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(final String correo) {
        this.correo = correo;
    }
}
