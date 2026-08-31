package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum TipoIdentificacionErrorCode implements ErrorDefinition {
    ERR_TIPO_IDENTIFICACION_NO_EXISTE("ERR_TIPO_IDENTIFICACION_NO_EXISTE", "El tipo de identificacion no existe.", ErrorKind.NOT_FOUND),
    ERR_TIPO_IDENTIFICACION_INVALIDA("ERR_TIPO_IDENTIFICACION_INVALIDA", "El tipo de identificacion no es valido.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_CODIGO_REQUERIDO("ERR_TIPO_IDENTIFICACION_CODIGO_REQUERIDO", "El codigo del tipo de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_CODIGO_LONGITUD_INVALIDA("ERR_TIPO_IDENTIFICACION_CODIGO_LONGITUD_INVALIDA", "El codigo del tipo de identificacion puede tener maximo 5 caracteres.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_NOMBRE_REQUERIDO("ERR_TIPO_IDENTIFICACION_NOMBRE_REQUERIDO", "El nombre del tipo de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA("ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA", "El nombre del tipo de identificacion puede tener maximo 50 caracteres.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_REQUERIDA("ERR_TIPO_IDENTIFICACION_REQUERIDA", "El tipo de identificacion es obligatorio.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    TipoIdentificacionErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
