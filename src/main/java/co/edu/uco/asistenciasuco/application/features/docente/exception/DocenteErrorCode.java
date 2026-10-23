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
    ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO("ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO", "El nombre completo del docente es obligatorio.", ErrorKind.VALIDATION),
    // HU091-HU093 (DOC_*): codigos de feature para que el cliente muestre el mensaje del backend y no uno generico.
    ERR_PERSONA_YA_REGISTRADA("ERR_PERSONA_YA_REGISTRADA", "Ya existe una persona registrada con ese documento o correo. Use la opción \"Vincular docente existente\".", ErrorKind.CONFLICT),
    ERR_TIPO_VINCULACION_INVALIDO("ERR_TIPO_VINCULACION_INVALIDO", "El tipo de vinculación indicado no existe o está inactivo.", ErrorKind.VALIDATION),
    ERR_MODO_VINCULACION_INVALIDO("ERR_MODO_VINCULACION_INVALIDO", "Debe indicar un docente existente o los datos de un docente nuevo, pero no ambos.", ErrorKind.VALIDATION),
    ERR_DOCENTE_YA_VINCULADO_PROGRAMA("ERR_DOCENTE_YA_VINCULADO_PROGRAMA", "El docente ya está vinculado a este programa.", ErrorKind.CONFLICT),
    ERR_BUSQUEDA_CANDIDATOS_CORTA("ERR_BUSQUEDA_CANDIDATOS_CORTA", "La búsqueda de candidatos requiere al menos 3 caracteres.", ErrorKind.VALIDATION),
    ERR_DOCENTE_NO_VINCULADO_PROGRAMA("ERR_DOCENTE_NO_VINCULADO_PROGRAMA", "El docente no está vinculado a este programa.", ErrorKind.NOT_FOUND),
    ERR_DOCENTE_SIN_VINCULO_ACTIVO("ERR_DOCENTE_SIN_VINCULO_ACTIVO", "El docente no tiene una vinculación activa con el programa.", ErrorKind.CONFLICT);

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
