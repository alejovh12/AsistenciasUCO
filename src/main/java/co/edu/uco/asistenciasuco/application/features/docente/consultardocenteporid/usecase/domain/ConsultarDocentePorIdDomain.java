package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Dominio de la operacion consultar docente por ID.
 */
public final class ConsultarDocentePorIdDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UUID docente;

    private ConsultarDocentePorIdDomain(final UUID docente) {
        this.docente = docente;
    }

    public static ConsultarDocentePorIdDomain crear(final UUID docente) {
        validarDocente(docente);
        return new ConsultarDocentePorIdDomain(docente);
    }

    private static void validarDocente(final UUID docente) {
        if (Objects.isNull(docente)) {
            throw new ValidationException("ERR_DOCENTE_REQUERIDO", "El docente es obligatorio.");
        }
        if (EMPTY_UUID.equals(docente)) {
            throw new ValidationException("ERR_DOCENTE_INVALIDO", "Debe seleccionar un docente valido.");
        }
    }

    public UUID getDocente() {
        return docente;
    }

}
