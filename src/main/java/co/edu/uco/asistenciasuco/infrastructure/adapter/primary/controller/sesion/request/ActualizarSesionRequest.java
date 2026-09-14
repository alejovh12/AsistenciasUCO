package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

public final class ActualizarSesionRequest {

    private String nombre;
    private String tema;
    private String descripcion;
    private String fechaHoraInicio;
    private String fechaHoraFin;
    private String aula;

    public String getNombre() {
        return nombre == null ? tema : nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public void setTema(final String tema) {
        this.tema = tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(final String descripcion) {
        this.descripcion = descripcion;
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

    public String getAula() {
        return aula;
    }

    public void setAula(final String aula) {
        this.aula = aula;
    }

    public void setRoom(final String room) {
        this.aula = room;
    }
}
