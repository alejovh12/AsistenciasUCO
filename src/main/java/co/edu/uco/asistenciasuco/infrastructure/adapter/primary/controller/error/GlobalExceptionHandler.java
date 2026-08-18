package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SAFE_INTERNAL_MESSAGE =
            "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.";
    private static final String SAFE_BAD_REQUEST_MESSAGE = "La solicitud no es valida.";

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            final ValidationException exception,
            final HttpServletRequest request
    ) {
        logControlled(HttpStatus.BAD_REQUEST, exception, request);
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            TypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            BindException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleFrameworkBadRequest(
            final Exception exception,
            final HttpServletRequest request
    ) {
        logFrameworkBadRequest(exception, request);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                SAFE_BAD_REQUEST_MESSAGE,
                request
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            final ForbiddenException exception,
            final HttpServletRequest request
    ) {
        logControlled(HttpStatus.FORBIDDEN, exception, request);
        return buildResponse(HttpStatus.FORBIDDEN, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            final ResourceNotFoundException exception,
            final HttpServletRequest request
    ) {
        logControlled(HttpStatus.NOT_FOUND, exception, request);
        return buildResponse(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            final ConflictException exception,
            final HttpServletRequest request
    ) {
        logControlled(HttpStatus.CONFLICT, exception, request);
        return buildResponse(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseOperation(
            final DatabaseOperationException exception,
            final HttpServletRequest request
    ) {
        logInternal(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getCode(), SAFE_INTERNAL_MESSAGE, request);
    }

    @ExceptionHandler(InternalApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleInternalApplication(
            final InternalApplicationException exception,
            final HttpServletRequest request
    ) {
        logInternal(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getCode(), SAFE_INTERNAL_MESSAGE, request);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(
            final ApplicationException exception,
            final HttpServletRequest request
    ) {
        logControlled(HttpStatus.UNPROCESSABLE_ENTITY, exception, request);
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(CrosscuttingException.class)
    public ResponseEntity<ApiErrorResponse> handleCrosscutting(
            final CrosscuttingException exception,
            final HttpServletRequest request
    ) {
        logInternal(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_APPLICATION_ERROR",
                SAFE_INTERNAL_MESSAGE,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            final Exception exception,
            final HttpServletRequest request
    ) {
        logInternal(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                SAFE_INTERNAL_MESSAGE,
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            final HttpStatus status,
            final String code,
            final String message,
            final HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                TextHelper.isNullOrBlank(message) ? status.getReasonPhrase() : message,
                request.getRequestURI(),
                CorrelationIdContext.getAsString()
        ));
    }

    private void logControlled(
            final HttpStatus status,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled application exception. status={}, path={}, correlationId={}, exceptionType={}, message={}",
                status.value(),
                request.getRequestURI(),
                CorrelationIdContext.getAsString(),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    private void logFrameworkBadRequest(
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled invalid request. status={}, path={}, correlationId={}, exceptionType={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                CorrelationIdContext.getAsString(),
                exception.getClass().getSimpleName()
        );
    }

    private void logInternal(
            final HttpStatus status,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.error(
                "Handled internal exception. status={}, path={}, correlationId={}, exceptionType={}",
                status.value(),
                request.getRequestURI(),
                CorrelationIdContext.getAsString(),
                exception.getClass().getSimpleName(),
                exception
        );
    }
}
