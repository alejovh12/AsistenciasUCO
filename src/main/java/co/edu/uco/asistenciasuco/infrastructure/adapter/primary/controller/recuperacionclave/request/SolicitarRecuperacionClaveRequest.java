package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request;

/** Cuerpo de {@code POST /api/v1/public/recuperacion-clave/solicitud}: el correo de la cuenta. */
public class SolicitarRecuperacionClaveRequest {

    private String correo;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(final String correo) {
        this.correo = correo;
    }

    /** No se imprime el correo: la ruta es publica y no debe dejar rastro de quien pidio el enlace. */
    @Override
    public String toString() {
        return "SolicitarRecuperacionClaveRequest[correo=***]";
    }
}
