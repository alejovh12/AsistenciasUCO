package co.edu.uco.asistenciasuco.application.features.estudiante.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum EstudianteErrorCode implements ErrorDefinition {
    ERR_ESTUDIANTE_NO_EXISTE("ERR_ESTUDIANTE_NO_EXISTE", "El estudiante consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_ESTUDIANTE_INACTIVO("ERR_ESTUDIANTE_INACTIVO", "El estudiante se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_IDENTIDAD_USUARIO_CONFLICTO("ERR_IDENTIDAD_USUARIO_CONFLICTO", "La identidad del usuario no coincide con el estudiante.", ErrorKind.CONFLICT),
    ERR_ESTUDIANTE_ID_REQUERIDO("ERR_ESTUDIANTE_ID_REQUERIDO", "El estudiante es obligatorio.", ErrorKind.VALIDATION),
    ERR_ESTUDIANTE_ID_INVALIDO("ERR_ESTUDIANTE_ID_INVALIDO", "El identificador del estudiante no es valido.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    EstudianteErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
