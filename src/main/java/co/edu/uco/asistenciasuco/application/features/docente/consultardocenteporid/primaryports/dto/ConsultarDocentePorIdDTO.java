package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar un docente por ID.
 */
public final class ConsultarDocentePorIdDTO {

    private UUID docente;

    public ConsultarDocentePorIdDTO() {
        super();
    }

    public ConsultarDocentePorIdDTO(final UUID docente) {
        setDocente(docente);
    }

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }

}
