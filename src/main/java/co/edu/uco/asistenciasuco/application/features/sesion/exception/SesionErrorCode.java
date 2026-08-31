package co.edu.uco.asistenciasuco.application.features.sesion.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum SesionErrorCode implements ErrorDefinition {
    ERR_SESION_NO_EXISTE("ERR_SESION_NO_EXISTE", "La sesion consultada no existe.", ErrorKind.NOT_FOUND),
    ERR_SESION_REQUERIDA("ERR_SESION_REQUERIDA", "La sesion es obligatoria.", ErrorKind.VALIDATION),
    ERR_TEMA_SESION_REQUERIDO("ERR_TEMA_SESION_REQUERIDO", "El tema de la sesion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TEMA_SESION_LONGITUD_INVALIDA("ERR_TEMA_SESION_LONGITUD_INVALIDA", "El tema de la sesion debe tener entre 5 y 100 caracteres.", ErrorKind.VALIDATION),
    ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA("ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA", "Cuando se indique una descripcion, debe tener entre 10 y 250 caracteres.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_REQUERIDA("ERR_OBSERVACION_CIERRE_REQUERIDA", "La observacion de cierre es obligatoria.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA("ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA", "La observacion de cierre debe tener entre 10 y 250 caracteres.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    SesionErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
