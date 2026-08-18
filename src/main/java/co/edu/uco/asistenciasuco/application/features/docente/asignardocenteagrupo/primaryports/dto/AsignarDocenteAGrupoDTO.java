package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para asignar o reasignar un docente a un grupo.
 */
public final class AsignarDocenteAGrupoDTO {

    private UUID docente;
    private UUID grupo;

    public AsignarDocenteAGrupoDTO() {
        super();
    }

    public AsignarDocenteAGrupoDTO(final UUID docente, final UUID grupo) {
        setDocente(docente);
        setGrupo(grupo);
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

}
