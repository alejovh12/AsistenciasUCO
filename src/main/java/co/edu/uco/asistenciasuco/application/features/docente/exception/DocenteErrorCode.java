package co.edu.uco.asistenciasuco.application.features.docente.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum DocenteErrorCode implements ErrorDefinition {
    ERR_DOCENTE_NO_EXISTE("ERR_DOCENTE_NO_EXISTE", "El docente consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_DOCENTE_INACTIVO("ERR_DOCENTE_INACTIVO", "El docente se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_DOCENTE_YA_REGISTRADO("ERR_DOCENTE_YA_REGISTRADO", "El usuario ya se encuentra registrado como docente.", ErrorKind.CONFLICT),
    ERR_DOCENTE_REQUERIDO("ERR_DOCENTE_REQUERIDO", "El docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_DOCENTE_INVALIDO("ERR_DOCENTE_INVALIDO", "El identificador del docente no es valido.", ErrorKind.VALIDATION),
    ERR_USUARIO_DOCENTE_REQUERIDO("ERR_USUARIO_DOCENTE_REQUERIDO", "El usuario del docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_USUARIO_DOCENTE_INVALIDO("ERR_USUARIO_DOCENTE_INVALIDO", "El identificador del usuario asociado al docente no es valido.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_DOCENTE_REQUERIDO("ERR_NUMERO_IDENTIFICACION_DOCENTE_REQUERIDO", "El numero de identificacion del docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO("ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO", "El nombre completo del docente es obligatorio.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    DocenteErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
