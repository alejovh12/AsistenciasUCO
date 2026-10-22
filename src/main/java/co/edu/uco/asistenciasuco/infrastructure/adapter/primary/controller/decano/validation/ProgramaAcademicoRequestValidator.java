package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation;

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
            throw new ValidationException("El cuerpo de la peticion es obligatorio.");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ValidationException("El nombre del programa academico es obligatorio.");
        }
        if (request.getNombre().trim().length() > NOMBRE_MAXIMO) {
            throw new ValidationException("El nombre del programa academico no puede superar " + NOMBRE_MAXIMO + " caracteres.");
        }
        if (request.getIdTipoPrograma() == null) {
            throw new ValidationException("El tipo de programa es obligatorio.");
        }
        if (request.getIdCoordinador() == null) {
            throw new ValidationException("El coordinador del programa es obligatorio.");
        }
    }

    public static void validar(final CambiarEstadoProgramaRequest request) {
        if (request == null || request.getEstaActivo() == null) {
            throw new ValidationException("El estado deseado del programa es obligatorio.");
        }
    }
}
