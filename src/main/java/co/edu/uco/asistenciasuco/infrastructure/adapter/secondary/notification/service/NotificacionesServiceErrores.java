package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traduce los errores del Servicio de Notificaciones a las excepciones y codigos que ya conoce el frontend: los mismos que
 * antes entregaban los procedimientos de plantillas y cola (NTF_001, NTF_004, NTF_005, NTF_008, NTF_020, NTF_021). El
 * {@code code} publico sigue siendo el generico de cada tipo (404, 409, 400) y el texto sale del catalogo por su
 * {@code messageCode}; para los rechazos de plantilla, cuyo mensaje del servicio es mas preciso (dice que marcador falta),
 * se usa un codigo de catalogo inexistente a proposito para que se conserve el texto del servicio.
 */
final class NotificacionesServiceErrores {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionesServiceErrores.class);
    private static final String PREFIJO_TEXTO_DEL_SERVICIO = "NTF_SERVICIO_";

    /**
     * 503 publico del servicio de notificaciones. Su texto de usuario sale de la fila existente del catalogo
     * {@code ERR_BD_NO_DISPONIBLE} («El servicio no esta disponible en este momento...»): asi no se necesita un codigo nuevo en
     * el seed de la BD. Si F5 siembra un codigo propio, solo cambia {@code messageCode()}.
     */
    static final ErrorDefinition NOTIFICACIONES_NO_DISPONIBLE = new ErrorDefinition() {
        @Override
        public String code() {
            return "ERR_NOTIFICACIONES_NO_DISPONIBLE";
        }

        @Override
        public String defaultMessage() {
            return "El servicio de notificaciones no está disponible en este momento. Intente de nuevo en unos minutos.";
        }

        @Override
        public ErrorKind kind() {
            return ErrorKind.SERVICE_UNAVAILABLE;
        }

        @Override
        public String messageCode() {
            return "ERR_BD_NO_DISPONIBLE";
        }
    };

    private NotificacionesServiceErrores() {
    }

    static RuntimeException traducir(final ServicioNotificacionesException e) {
        final int status = e.status();
        final String code = e.code();
        if (status == 401 || status == 403) {
            LOGGER.error("El servicio de notificaciones rechazo las credenciales de administracion (HTTP {}, code={}): "
                    + "revise NOTIFICACIONES_API_KEY.", status, code);
            return noDisponible(e);
        }
        if (status >= 500 || status == 408 || status == 429) {
            return noDisponible(e);
        }
        if (status == 404) {
            return new ResourceNotFoundException(definicion(CommonErrorCode.RESOURCE_NOT_FOUND, codigoCatalogoNoEncontrado(code), e));
        }
        if (status == 409) {
            return new ConflictException(definicion(CommonErrorCode.CONFLICT, "NTF_008", e));
        }
        return new ValidationException(definicion(CommonErrorCode.VALIDATION_ERROR, codigoCatalogoValidacion(code), e));
    }

    static InternalApplicationException noDisponible(final Throwable causa) {
        return InternalApplicationException.servicioNoDisponible(NOTIFICACIONES_NO_DISPONIBLE, causa);
    }

    private static String codigoCatalogoNoEncontrado(final String code) {
        return switch (code) {
            case "TIPO_NO_ENCONTRADO" -> "NTF_021";
            case "VERSION_NO_ENCONTRADA" -> "NTF_005";
            case "NOTIFICACION_NO_ENCONTRADA" -> "NTF_020";
            default -> null;
        };
    }

    private static String codigoCatalogoValidacion(final String code) {
        if (code.startsWith("PLANTILLA_")) {
            return PREFIJO_TEXTO_DEL_SERVICIO + code;
        }
        return "NTF_001";
    }

    private static ErrorDefinition definicion(final ErrorDefinition base, final String codigoCatalogo, final ServicioNotificacionesException e) {
        final String mensaje = e.mensajeServicio().isBlank() ? base.defaultMessage() : e.mensajeServicio();
        return new RechazoDelServicio(base, codigoCatalogo == null ? base.code() : codigoCatalogo, mensaje);
    }

    /** Mismo codigo publico y tipo que {@code base}; el texto de usuario se busca por {@code codigoCatalogo}. */
    private record RechazoDelServicio(ErrorDefinition base, String codigoCatalogo, String mensaje) implements ErrorDefinition {

        @Override
        public String code() {
            return base.code();
        }

        @Override
        public String defaultMessage() {
            return mensaje;
        }

        @Override
        public ErrorKind kind() {
            return base.kind();
        }

        @Override
        public String messageCode() {
            return codigoCatalogo;
        }
    }
}
