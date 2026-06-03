package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para registrar una asistencia.
 */
public final class RegistrarAsistenciaRepositoryDTO {

    private UUID estudiante;
    private UUID grupo;
    private UUID sesion;
    private Boolean presente;
    private String observacion;
    private UUID idCorrelacion;

    public RegistrarAsistenciaRepositoryDTO() {
        super();
    }

    public RegistrarAsistenciaRepositoryDTO(
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final Boolean presente,
            final String observacion,
            final UUID idCorrelacion
    ) {
        setEstudiante(estudiante);
        setGrupo(grupo);
        setSesion(sesion);
        setPresente(presente);
        setObservacion(observacion);
        setIdCorrelacion(idCorrelacion);
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(final UUID estudiante) {
        this.estudiante = estudiante;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(final Boolean presente) {
        this.presente = presente;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(final String observacion) {
        this.observacion = observacion;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
