package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import java.util.UUID;

public final class AsignarDocenteAGrupoRequest {

    private UUID docente;
    private UUID grupo;

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
