package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.ErrorResponse;
import co.edu.uco.notificaciones.infrastructure.web.CorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduce toda falla al cuerpo uniforme {code, message, correlationId}. Nunca expone trazas ni detalles de infraestructura. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<ErrorResponse> negocio(final ErrorNegocio ex) {
        final HttpStatus estado = switch (ex.categoria()) {
            case VALIDACION -> HttpStatus.BAD_REQUEST;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLICTO -> HttpStatus.CONFLICT;
        };
        return responder(estado, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacion(final MethodArgumentNotValidException ex) {
        final String detalle = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map((FieldError f) -> f.getField() + ": " + f.getDefaultMessage()).orElse("la solicitud no es valida");
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "La solicitud no es valida (" + detalle + ").");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> cuerpoIlegible(final HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO", "El cuerpo de la solicitud no es un JSON valido.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> parametro(final Exception ex) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Un parametro de la solicitud falta o no es valido.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> metodo(final HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", "El metodo HTTP no esta permitido en este recurso.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noEncontrado(final NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "El recurso solicitado no existe.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> inesperado(final Exception ex) {
        LOGGER.error("Error inesperado: {}", ex.getMessage(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "No fue posible procesar la solicitud. Intente de nuevo mas tarde.");
    }

    private static ResponseEntity<ErrorResponse> responder(final HttpStatus estado, final String codigo, final String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorResponse(codigo, mensaje, MDC.get(CorrelationFilter.MDC_CLAVE)));
    }
}
