package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.UUID;

/**
 * Entidad del puerto secundario para resultados de sesion.
 */
public final class SesionRepositoryEntity {

    private final UUID sesion;
    private final UUID grupo;
    private final String tema;
    private final String descripcion;
    private final boolean cerrada;
    private final String observacionCierre;

    public SesionRepositoryEntity(
            final UUID sesion,
            final UUID grupo,
            final String tema,
            final String descripcion,
            final boolean cerrada,
            final String observacionCierre
    ) {
        this.sesion = sesion;
        this.grupo = grupo;
        this.tema = tema;
        this.descripcion = descripcion;
        this.cerrada = cerrada;
        this.observacionCierre = observacionCierre;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public String getTema() {
        return tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isCerrada() {
        return cerrada;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }
}
