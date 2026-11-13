package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import org.springframework.http.HttpStatus;

final class ApiErrorCatalog {

    private ApiErrorCatalog() {
    }

    static ApiErrorDescriptor fromApplicationException(final ApplicationException exception) {
        return exception.getErrorDefinition()
                .map(definition -> fromDefinition(definition, exception))
                .orElseGet(() -> fromUnknownApplicationException(exception));
    }

    /**
     * VAL-oleada3 MSG-05: el codigo publico y el estado salen de la definicion; el texto se busca en el catalogo por su
     * {@code messageCode}. Con parametros, el respaldo es el texto ya llenado de la excepcion. Funcionalidad no disponible
     * conserva el 501 de su tipo de excepcion.
     */
    private static ApiErrorDescriptor fromDefinition(final ErrorDefinition definition, final ApplicationException exception) {
        final boolean conParametros = !exception.getMessageArgs().isEmpty();
        final String message = conParametros ? exception.getMessage() : definition.defaultMessage();
        final HttpStatus status = exception instanceof FeatureUnavailableException
                ? HttpStatus.NOT_IMPLEMENTED
                : statusFor(definition.kind());
        return new ApiErrorDescriptor(definition.code(), message, status, definition.messageCode(), exception.getMessageArgs());
    }

    static ApiErrorDescriptor fromTechnicalException(final TechnicalException exception) {
        return exception.getErrorDefinition()
                .map(ApiErrorCatalog::from)
                .orElseGet(ApiErrorCatalog::internalError);
    }

    static ApiErrorDescriptor invalidRequest() {
        return from(CommonErrorCode.INVALID_REQUEST);
    }

    static ApiErrorDescriptor requestValidationError() {
        return from(CommonErrorCode.VALIDATION_ERROR);
    }

    /**
     * 400 de validacion con un codigo de catalogo propio (CULM-001 D.9-a, p. ej. {@code PAG_001}): el texto institucional lo
     * resuelve el catalogo de mensajes por ese codigo; el de aqui es solo la contingencia ante desconexion.
     */
    static ApiErrorDescriptor validationWithCode(final String code) {
        return new ApiErrorDescriptor(code, "Los parametros de la solicitud no son validos.", HttpStatus.BAD_REQUEST);
    }

    /**
     * Multipart rechazado por {@code spring.servlet.multipart.max-file-size}: mismo rechazo de
     * cliente que aplica la validacion de contenido de soportes (LB-004 CONTENT-005).
     */
    static ApiErrorDescriptor uploadSizeExceeded() {
        return new ApiErrorDescriptor(
                CommonErrorCode.VALIDATION_ERROR.code(),
                "El archivo excede el tamano maximo permitido (5 MiB).",
                HttpStatus.BAD_REQUEST
        );
    }

    static ApiErrorDescriptor unsupportedMediaType() {
        return new ApiErrorDescriptor(
                CommonErrorCode.INVALID_REQUEST.code(),
                "El tipo de contenido de la solicitud no es compatible.",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );
    }

    /** CULM-001 D.9-c (F-TRX-019): el {@code Accept} del cliente no admite ningun formato que el endpoint produce. */
    static ApiErrorDescriptor notAcceptable() {
        return new ApiErrorDescriptor(
                CommonErrorCode.INVALID_REQUEST.code(),
                "El formato de respuesta solicitado no esta disponible para este recurso.",
                HttpStatus.NOT_ACCEPTABLE
        );
    }

    static ApiErrorDescriptor malformedMultipart() {
        return new ApiErrorDescriptor(
                CommonErrorCode.INVALID_REQUEST.code(),
                "El cuerpo multipart no tiene un formato valido.",
                HttpStatus.BAD_REQUEST
        );
    }

    static ApiErrorDescriptor resourceNotFound() {
        return from(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    static ApiErrorDescriptor internalError() {
        return from(CommonErrorCode.INTERNAL_ERROR);
    }

    /** Metodo HTTP no soportado por una ruta existente (p. ej. PUT en /api/v2/sesiones/{id}, UTC-D09). */
    static ApiErrorDescriptor methodNotAllowed() {
        return new ApiErrorDescriptor(
                "METHOD_NOT_ALLOWED",
                "El metodo HTTP no esta soportado para este recurso.",
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    static ApiErrorDescriptor from(final ErrorDefinition errorDefinition) {
        return new ApiErrorDescriptor(errorDefinition.code(), errorDefinition.defaultMessage(), statusFor(errorDefinition.kind()));
    }

    private static ApiErrorDescriptor fromUnknownApplicationException(final ApplicationException exception) {
        final HttpStatus status = statusForExceptionType(exception);
        final String message = TextHelper.isNullOrBlank(exception.getMessage())
                ? CommonErrorCode.BUSINESS_ERROR.defaultMessage()
                : exception.getMessage();
        return new ApiErrorDescriptor(exception.getCode(), message, status);
    }

    private static HttpStatus statusForExceptionType(final ApplicationException exception) {
        if (exception instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof FeatureUnavailableException) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        if (exception instanceof InternalApplicationException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }

    private static HttpStatus statusFor(final ErrorKind kind) {
        return switch (kind) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case BUSINESS -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

}
