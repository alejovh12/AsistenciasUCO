package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar asignaciones academicas de un docente.
 */
public final class ConsultarAsignacionesAcademicasDocenteDTO {

    private UUID docente;

    public ConsultarAsignacionesAcademicasDocenteDTO() {
        super();
    }

    public ConsultarAsignacionesAcademicasDocenteDTO(final UUID docente) {
        setDocente(docente);
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

}
