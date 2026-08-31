package co.edu.uco.asistenciasuco.application.features.asistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum AsistenciaErrorCode implements ErrorDefinition {
    ERR_ESTUDIANTE_NO_PERTENECE_SESION("ERR_ESTUDIANTE_NO_PERTENECE_SESION", "El estudiante no pertenece a la sesion.", ErrorKind.FORBIDDEN),
    ERR_ASISTENCIA_REQUERIDA("ERR_ASISTENCIA_REQUERIDA", "La asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_PRESENTE_REQUERIDO("ERR_PRESENTE_REQUERIDO", "Debe indicar si el estudiante asistio.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA("ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA", "Cuando se indique una observacion de asistencia, debe tener entre 5 y 250 caracteres.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_REQUERIDA("ERR_OBSERVACION_ASISTENCIA_REQUERIDA", "Debe indicar una observacion cuando el estudiante no asiste.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_REQUERIDO("ERR_MOTIVO_REVISION_REQUERIDO", "El motivo de revision es obligatorio.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_LONGITUD_INVALIDA("ERR_MOTIVO_REVISION_LONGITUD_INVALIDA", "El motivo de revision debe tener entre 10 y 300 caracteres.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AsistenciaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.kind = kind;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
