package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para asignar o reasignar un docente a un grupo.
 */
public final class AsignarDocenteAGrupoRepositoryDTO {

    private UUID docente;
    private UUID grupo;

    public AsignarDocenteAGrupoRepositoryDTO() {
        super();
    }

    public AsignarDocenteAGrupoRepositoryDTO(
            final UUID docente,
            final UUID grupo
    ) {
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
