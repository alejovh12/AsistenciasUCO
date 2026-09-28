package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para cerrar una sesion.
 */
public final class CerrarSesionRepositoryDTO {

    private UUID sesion;
    private UUID docente;
    private String observacionCierre;
    private UUID usuarioEjecutor;

    public CerrarSesionRepositoryDTO() {
        super();
    }

    public CerrarSesionRepositoryDTO(
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
