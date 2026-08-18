package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para crear una sesion.
 */
public final class CrearSesionDTO {

    private UUID grupo;
    private String tema;
    private String descripcion;

    public CrearSesionDTO() {
        super();
    }

    public CrearSesionDTO(
            final UUID grupo,
            final String tema,
            final String descripcion
    ) {
        setGrupo(grupo);
        setTema(tema);
        setDescripcion(descripcion);
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

}
