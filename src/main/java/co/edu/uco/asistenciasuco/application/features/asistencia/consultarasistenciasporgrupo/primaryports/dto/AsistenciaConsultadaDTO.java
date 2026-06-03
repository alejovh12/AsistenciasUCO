package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida para una asistencia consultada.
 */
public final class AsistenciaConsultadaDTO {

    private UUID asistencia;
    private UUID estudiante;
    private UUID grupo;
    private UUID sesion;
    private Boolean presente;
    private String observacion;

    public AsistenciaConsultadaDTO() {
        super();
    }

    public AsistenciaConsultadaDTO(
            final UUID asistencia,
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final Boolean presente,
            final String observacion
    ) {
        setAsistencia(asistencia);
        setEstudiante(estudiante);
        setGrupo(grupo);
        setSesion(sesion);
        setPresente(presente);
        setObservacion(observacion);
    }

    public UUID getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(final UUID asistencia) {
        this.asistencia = asistencia;
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
}
