package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Dominio de la operacion consultar asignaciones academicas de un docente.
 */
public final class ConsultarAsignacionesAcademicasDocenteDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UUID docente;

    private ConsultarAsignacionesAcademicasDocenteDomain(final UUID docente) {
        this.docente = docente;
    }

    public static ConsultarAsignacionesAcademicasDocenteDomain crear(final UUID docente) {
        validarDocente(docente);
        return new ConsultarAsignacionesAcademicasDocenteDomain(docente);
    }

    private static void validarDocente(final UUID docente) {
        if (Objects.isNull(docente)) {
            throw new ValidationException(ErrorCode.ERR_DOCENTE_REQUERIDO);
        }
        if (EMPTY_UUID.equals(docente)) {
            throw new ValidationException(ErrorCode.ERR_DOCENTE_INVALIDO);
        }
    }

    public UUID getDocente() {
        return docente;
    }

}
