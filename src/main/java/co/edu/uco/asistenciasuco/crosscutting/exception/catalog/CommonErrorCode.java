package co.edu.uco.asistenciasuco.crosscutting.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum CommonErrorCode implements ErrorDefinition {
    VALIDATION_ERROR("VALIDATION_ERROR", "Hay campos con informacion invalida. Revise los datos enviados.", ErrorKind.VALIDATION),
    INVALID_REQUEST("INVALID_REQUEST", "No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados.", ErrorKind.VALIDATION),
    BUSINESS_ERROR("BUSINESS_ERROR", "No fue posible completar la operacion solicitada.", ErrorKind.BUSINESS),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "El recurso solicitado no existe.", ErrorKind.NOT_FOUND),
    CONFLICT("CONFLICT", "La operacion genera un conflicto con el estado actual.", ErrorKind.CONFLICT),
    INTERNAL_ERROR("INTERNAL_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    INTERNAL_APPLICATION_ERROR("INTERNAL_APPLICATION_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    ERR_PAGE_INVALIDA("ERR_PAGE_INVALIDA", "La pagina debe ser mayor o igual que cero.", ErrorKind.VALIDATION),
    ERR_SIZE_INVALIDO("ERR_SIZE_INVALIDO", "El tamano de pagina debe estar entre 1 y 100.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    CommonErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
