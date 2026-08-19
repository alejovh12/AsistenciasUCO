package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ErrorKind;
import co.edu.uco.asistenciasuco.application.exception.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import org.springframework.http.HttpStatus;

final class ApiErrorCatalog {

    private ApiErrorCatalog() {
        throw new IllegalStateException("No es permitido instanciar el catalogo de errores HTTP.");
    }

    static ApiErrorDescriptor fromApplicationException(final ApplicationException exception) {
        final ErrorCode errorCode = exception.getErrorCode().orElse(null);
        if (errorCode == null) {
            return fromUnknownApplicationException(exception);
        }
        return from(errorCode);
    }

    static ApiErrorDescriptor invalidRequest() {
        return from(ErrorCode.INVALID_REQUEST);
    }

    static ApiErrorDescriptor resourceNotFound() {
        return from(ErrorCode.RESOURCE_NOT_FOUND);
    }

    static ApiErrorDescriptor internalError() {
        return from(ErrorCode.INTERNAL_ERROR);
    }

    static ApiErrorDescriptor from(final ErrorCode errorCode) {
        return new ApiErrorDescriptor(errorCode.code(), errorCode.defaultMessage(), statusFor(errorCode.kind()));
    }

    private static ApiErrorDescriptor fromUnknownApplicationException(final ApplicationException exception) {
        final HttpStatus status = statusForExceptionType(exception);
        final String message = SensitiveDataSanitizer.sanitizePublicMessage(
                exception.getMessage(),
                ErrorCode.BUSINESS_ERROR.defaultMessage()
        );
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
        if (exception instanceof InternalApplicationException || exception instanceof DatabaseOperationException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    private static HttpStatus statusFor(final ErrorKind kind) {
        return switch (kind) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case BUSINESS -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

}
