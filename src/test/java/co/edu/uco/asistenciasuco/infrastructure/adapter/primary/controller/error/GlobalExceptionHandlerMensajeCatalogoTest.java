package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VAL-oleada3 MSG-05: los errores de {@link MensajeUsuarioErrorCode} conservan el codigo publico y el estado HTTP, y su texto
 * sale del catalogo de mensajes por el codigo de mensaje (con los marcadores llenos); sin catalogo, cae al texto de respaldo.
 */
class GlobalExceptionHandlerMensajeCatalogoTest {

    private static final Map<String, String> CATALOGO = Map.of(
            "VAL_APP_001", "Los datos de la facultad son obligatorios (catálogo).",
            "VAL_APP_039", "El tamaño de página debe estar entre 1 y {}.",
            "ACC_APP_003", "No fue posible resolver el docente autenticado (catálogo).",
            "VAL_APP_069", "El campo {} es obligatorio (catálogo).");

    private final GlobalExceptionHandler conCatalogo =
            new GlobalExceptionHandler(codigo -> Optional.ofNullable(CATALOGO.get(codigo)));
    private final GlobalExceptionHandler sinCatalogo = new GlobalExceptionHandler(codigo -> Optional.empty());

    @Test
    void validacion_conserva_validation_error_y_toma_el_texto_del_catalogo() {
        final ResponseEntity<ApiErrorResponse> respuesta = conCatalogo.handleApplication(
                new ValidationException(MensajeUsuarioErrorCode.VAL_APP_001), request());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("VALIDATION_ERROR", respuesta.getBody().code());
        assertEquals("Los datos de la facultad son obligatorios (catálogo).", respuesta.getBody().message());
    }

    @Test
    void los_parametros_llenan_el_marcador_del_texto_del_catalogo() {
        final ResponseEntity<ApiErrorResponse> respuesta = conCatalogo.handleApplication(
                ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_039, 100), request());

        assertEquals("VALIDATION_ERROR", respuesta.getBody().code());
        assertEquals("El tamaño de página debe estar entre 1 y 100.", respuesta.getBody().message());
    }

    @Test
    void un_codigo_propio_de_respuesta_se_conserva_con_texto_del_catalogo() {
        final ResponseEntity<ApiErrorResponse> respuesta = conCatalogo.handleApplication(
                ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069, "dias"), request());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("ERR_CAMPO_OBLIGATORIO", respuesta.getBody().code());
        assertEquals("El campo dias es obligatorio (catálogo).", respuesta.getBody().message());
    }

    @Test
    void acceso_conserva_forbidden_y_403() {
        final ResponseEntity<ApiErrorResponse> respuesta = conCatalogo.handleApplication(
                new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_003), request());

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("FORBIDDEN", respuesta.getBody().code());
        assertEquals("No fue posible resolver el docente autenticado (catálogo).", respuesta.getBody().message());
    }

    @Test
    void sin_catalogo_responde_el_texto_de_respaldo_con_parametros() {
        final ResponseEntity<ApiErrorResponse> respuesta = sinCatalogo.handleApplication(
                ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_039, 50), request());

        assertEquals("El tamano de pagina debe estar entre 1 y 50.", respuesta.getBody().message());
    }

    @Test
    void sin_catalogo_un_recurso_inexistente_responde_404_con_su_texto() {
        final ResponseEntity<ApiErrorResponse> respuesta = sinCatalogo.handleApplication(
                new ResourceNotFoundException(MensajeUsuarioErrorCode.NOF_APP_014), request());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", respuesta.getBody().code());
        assertEquals("El archivo solicitado no existe.", respuesta.getBody().message());
    }

    @Test
    void funcionalidad_no_disponible_de_catalogo_conserva_501_y_su_codigo() {
        final ResponseEntity<ApiErrorResponse> respuesta = sinCatalogo.handleApplication(
                new FeatureUnavailableException(MensajeUsuarioErrorCode.DIS_APP_001), request());

        assertEquals(HttpStatus.NOT_IMPLEMENTED, respuesta.getStatusCode());
        assertEquals("FEATURE_UNAVAILABLE", respuesta.getBody().code());
        assertEquals(MensajeUsuarioErrorCode.DIS_APP_001.defaultMessage(), respuesta.getBody().message());
    }

    @Test
    void la_excepcion_con_parametros_expone_mensaje_lleno_y_sus_argumentos() {
        final ValidationException excepcion = ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_034, "accion");

        assertEquals("El filtro accion excede la longitud permitida.", excepcion.getMessage());
        assertEquals(List.of("accion"), excepcion.getMessageArgs());
        assertEquals("VALIDATION_ERROR", excepcion.getCode());
    }

    private static MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/prueba");
        return request;
    }
}
