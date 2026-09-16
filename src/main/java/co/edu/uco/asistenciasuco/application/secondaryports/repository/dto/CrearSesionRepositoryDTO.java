package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * DTO del puerto secundario para crear una sesion.
 */
public final class CrearSesionRepositoryDTO {

    private UUID grupo;
    private String tema;
    private String descripcion;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private String aula;
    private String tipo;
    private UUID docente;
    private UUID usuarioEjecutor;

    public CrearSesionRepositoryDTO() {
        super();
    }

    public CrearSesionRepositoryDTO(
            final UUID grupo,
            final String tema,
            final String descripcion,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String aula,
            final String tipo,
            final UUID docente,
            final UUID usuarioEjecutor
    ) {
        setGrupo(grupo);
        setTema(tema);
        setDescripcion(descripcion);
        setFechaHoraInicio(fechaHoraInicio);
        setFechaHoraFin(fechaHoraFin);
        setAula(aula);
        setTipo(tipo);
        setDocente(docente);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public String getTema() {
        return tema;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(final String tipo) {
        this.tipo = tipo;
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
