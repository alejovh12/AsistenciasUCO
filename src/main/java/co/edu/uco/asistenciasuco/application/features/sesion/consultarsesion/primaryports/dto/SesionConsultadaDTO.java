package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida para una sesion consultada.
 */
public final class SesionConsultadaDTO {

    private UUID sesion;
    private UUID grupo;
    private String tema;
    private String descripcion;
    private Boolean cerrada;
    private String observacionCierre;

    public SesionConsultadaDTO() {
        super();
    }

    public SesionConsultadaDTO(
            final UUID sesion,
            final UUID grupo,
            final String tema,
            final String descripcion,
            final Boolean cerrada,
            final String observacionCierre
    ) {
        setSesion(sesion);
        setGrupo(grupo);
        setTema(tema);
        setDescripcion(descripcion);
        setCerrada(cerrada);
        setObservacionCierre(observacionCierre);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
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

    public Boolean getCerrada() {
        return cerrada;
    }

    public void setCerrada(final Boolean cerrada) {
        this.cerrada = cerrada;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(final String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }
}
