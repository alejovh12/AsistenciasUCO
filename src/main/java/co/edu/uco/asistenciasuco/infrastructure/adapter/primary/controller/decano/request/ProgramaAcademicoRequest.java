package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

import java.util.UUID;

/** Cuerpo de alta (POST) y reemplazo completo (PUT) de un programa academico. */
public final class ProgramaAcademicoRequest {
    private String nombre;
    private UUID idTipoPrograma;
    private UUID idCoordinador;
    private UUID idModalidad;
    private UUID idJornada;

    public String getNombre() { return nombre; }
    public void setNombre(final String nombre) { this.nombre = nombre; }
    public UUID getIdTipoPrograma() { return idTipoPrograma; }
    public void setIdTipoPrograma(final UUID idTipoPrograma) { this.idTipoPrograma = idTipoPrograma; }
    public UUID getIdCoordinador() { return idCoordinador; }
    public void setIdCoordinador(final UUID idCoordinador) { this.idCoordinador = idCoordinador; }
    public UUID getIdModalidad() { return idModalidad; }
    public void setIdModalidad(final UUID idModalidad) { this.idModalidad = idModalidad; }
    public UUID getIdJornada() { return idJornada; }
    public void setIdJornada(final UUID idJornada) { this.idJornada = idJornada; }
}
