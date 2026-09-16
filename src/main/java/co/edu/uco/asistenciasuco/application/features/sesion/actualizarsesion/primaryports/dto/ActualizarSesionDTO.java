package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ActualizarSesionDTO {

    private UUID sesion;
    private String nombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private String aula;
    private String descripcion;
    private UUID docente;
    private UUID usuarioEjecutor;

    public ActualizarSesionDTO() {
        super();
    }

    public ActualizarSesionDTO(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String aula,
            final String descripcion,
            final UUID docente,
            final UUID usuarioEjecutor
    ) {
        setSesion(sesion);
        setNombre(nombre);
        setFechaHoraInicio(fechaHoraInicio);
        setFechaHoraFin(fechaHoraFin);
        setAula(aula);
        setDescripcion(descripcion);
        setDocente(docente);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(final LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(final LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(final String aula) {
        this.aula = aula;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(final String descripcion) {
        this.descripcion = descripcion;
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }
}
