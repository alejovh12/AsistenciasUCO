package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

public final class ActualizarSesionRequest {

    private String nombre;
    private String fechaHoraInicio;
    private String fechaHoraFin;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public String getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(final String fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(final String fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }
}
