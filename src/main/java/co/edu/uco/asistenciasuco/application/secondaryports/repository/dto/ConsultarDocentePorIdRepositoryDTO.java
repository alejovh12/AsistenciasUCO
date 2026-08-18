package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar un docente por ID.
 */
public final class ConsultarDocentePorIdRepositoryDTO {

    private UUID docente;

    public ConsultarDocentePorIdRepositoryDTO() {
        super();
    }

    public ConsultarDocentePorIdRepositoryDTO(final UUID docente) {
        setDocente(docente);
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

}
