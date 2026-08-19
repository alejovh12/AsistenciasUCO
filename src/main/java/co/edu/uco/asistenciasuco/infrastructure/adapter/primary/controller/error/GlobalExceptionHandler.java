package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.invalidRequest();
        logFrameworkBadRequest(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            final NoResourceFoundException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.resourceNotFound();
        logFrameworkNotFound(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(
            final ApplicationException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromApplicationException(exception);
        if (descriptor.status().is5xxServerError()) {
            logInternal(descriptor, exception, request);
        } else {
            logControlled(descriptor, exception, request);
        }
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(CrosscuttingException.class)
    public ResponseEntity<ApiErrorResponse> handleCrosscutting(
            final CrosscuttingException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.internalError();
        logInternal(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            final Exception exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.internalError();
        logInternal(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(final ApiErrorDescriptor descriptor, final HttpServletRequest request) {
        AuditRequestAttributes.storeErrorCode(request, descriptor.code());
        return ResponseEntity.status(descriptor.status()).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                descriptor.status().value(),
                descriptor.status().getReasonPhrase(),
                descriptor.code(),
                TextHelper.isNullOrBlank(descriptor.message()) ? descriptor.status().getReasonPhrase() : descriptor.message(),
                safePath(request),
                CorrelationIdContext.getAsString()
        ));
    }

    private void logControlled(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled application exception. status={}, code={}, path={}, correlationId={}, message={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                SensitiveDataSanitizer.sanitizeForLog(exception.getMessage())
        );
    }

    private void logFrameworkBadRequest(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled invalid request. status={}, code={}, path={}, correlationId={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString()
        );
    }

    private void logFrameworkNotFound(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled resource not found. status={}, code={}, path={}, correlationId={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString()
        );
    }

    private void logInternal(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.error(
                "Handled internal exception. status={}, code={}, path={}, correlationId={}, stackTrace={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                safeStackTrace(exception)
        );
    }

    private String safePath(final HttpServletRequest request) {
        return SensitiveDataSanitizer.sanitizeForLog(request.getRequestURI(), 240);
    }

    private String safeStackTrace(final Exception exception) {
        final StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return SensitiveDataSanitizer.sanitizeForLog(writer.toString(), 8_000);
    }
}
