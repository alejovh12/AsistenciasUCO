package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para crear una sesion.
 */
public final class CrearSesionRepositoryDTO {

    private UUID grupo;
    private String tema;
    private String descripcion;
    private UUID idCorrelacion;

    public CrearSesionRepositoryDTO() {
        super();
    }

    public CrearSesionRepositoryDTO(
            final UUID grupo,
            final String tema,
            final String descripcion,
            final UUID idCorrelacion
    ) {
        setGrupo(grupo);
        setTema(tema);
        setDescripcion(descripcion);
        setIdCorrelacion(idCorrelacion);
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
