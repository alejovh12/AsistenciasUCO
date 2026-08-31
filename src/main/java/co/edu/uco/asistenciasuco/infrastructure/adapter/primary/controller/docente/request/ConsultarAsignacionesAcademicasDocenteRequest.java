package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import java.util.UUID;

public final class ConsultarAsignacionesAcademicasDocenteRequest {

    private UUID docente;

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }
}
