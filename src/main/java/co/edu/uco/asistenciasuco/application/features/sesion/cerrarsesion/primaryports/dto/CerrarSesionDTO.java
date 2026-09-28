package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para cerrar una sesion.
 */
public final class CerrarSesionDTO {

    private UUID sesion;
    private UUID docente;
    private String observacionCierre;
    private UUID usuarioEjecutor;

    public CerrarSesionDTO() {
        super();
    }

    public CerrarSesionDTO(
            final UUID sesion,
            final UUID docente,
            final String observacionCierre,
            final UUID usuarioEjecutor
    ) {
        setSesion(sesion);
        setDocente(docente);
        setObservacionCierre(observacionCierre);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(final String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }

}
