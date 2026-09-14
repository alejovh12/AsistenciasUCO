package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

import java.util.UUID;

public final class CrearSesionRequest {

    private UUID grupo;
    private String tema;
    private String nombre;
    private String descripcion;
    private String fechaHoraInicio;
    private String fechaHoraFin;
    private String aula;
    private String tipo;

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public String getTema() {
        return tema == null ? nombre : tema;
    }

    public void setTema(final String tema) {
        this.tema = tema;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(final String tipo) {
        this.tipo = tipo;
    }
}
