package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.util.UUID;

/**
 * Entidad del puerto secundario para resultados de asistencia.
 */
public final class AsistenciaRepositoryEntity {

    private final UUID asistencia;
    private final UUID estudiante;
    private final UUID grupo;
    private final UUID sesion;
    private final boolean presente;
    private final String observacion;

    public AsistenciaRepositoryEntity(
            final UUID asistencia,
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final boolean presente,
            final String observacion
    ) {
        this.asistencia = asistencia;
        this.estudiante = estudiante;
        this.grupo = grupo;
        this.sesion = sesion;
        this.presente = presente;
        this.observacion = observacion;
    }

    public UUID getAsistencia() {
        return asistencia;
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public boolean isPresente() {
        return presente;
    }

    public String getObservacion() {
        return observacion;
    }
}
