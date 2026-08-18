package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar asignaciones academicas de un docente.
 */
public final class ConsultarAsignacionesAcademicasDocenteRepositoryDTO {

    private UUID docente;

    public ConsultarAsignacionesAcademicasDocenteRepositoryDTO() {
        super();
    }

    public ConsultarAsignacionesAcademicasDocenteRepositoryDTO(final UUID docente) {
        setDocente(docente);
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

}
