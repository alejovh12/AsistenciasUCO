package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;

import java.util.UUID;

public record ConsultarEstudiantePorIdDomain(UUID estudianteId) {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public static ConsultarEstudiantePorIdDomain crear(final UUID estudianteId) {
        if (estudianteId == null) {
            throw new ValidationException(ErrorCode.ERR_ESTUDIANTE_ID_REQUERIDO);
        }
        if (EMPTY_UUID.equals(estudianteId)) {
            throw new ValidationException(ErrorCode.ERR_ESTUDIANTE_ID_INVALIDO);
        }
        return new ConsultarEstudiantePorIdDomain(estudianteId);
    }
}
