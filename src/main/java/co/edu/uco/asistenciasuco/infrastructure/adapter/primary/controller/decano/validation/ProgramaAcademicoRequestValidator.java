package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CambiarEstadoProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.ProgramaAcademicoRequest;

/** Validacion de forma de las peticiones HTTP del programa academico (400 sin llegar al caso de uso). */
public final class ProgramaAcademicoRequestValidator {

    private static final int NOMBRE_MAXIMO = 50;

    private ProgramaAcademicoRequestValidator() {
    }

    public static void validar(final ProgramaAcademicoRequest request) {
        if (request == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_063);
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_045);
        }
        if (request.getNombre().trim().length() > NOMBRE_MAXIMO) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_046, NOMBRE_MAXIMO);
        }
        if (request.getIdTipoPrograma() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_064);
        }
        if (request.getIdCoordinador() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_065);
        }
    }

    public static void validar(final CambiarEstadoProgramaRequest request) {
        if (request == null || request.getEstaActivo() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_066);
        }
    }
}
