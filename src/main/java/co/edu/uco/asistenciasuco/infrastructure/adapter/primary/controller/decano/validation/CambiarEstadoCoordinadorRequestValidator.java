package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CambiarEstadoCoordinadorRequest;

/**
 * Validacion de forma de {@code PATCH /api/v1/decano/coordinadores/{id}/estado}: el estado destino es obligatorio.
 */
public final class CambiarEstadoCoordinadorRequestValidator implements Validator<CambiarEstadoCoordinadorRequest> {

    @Override
    public ValidationResult validate(final CambiarEstadoCoordinadorRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "El estado del coordinador es obligatorio.").build();
        }
        if (request.getEstaActivo() == null) {
            builder.add("estaActivo", ValidationErrorType.REQUIRED, "El estado destino del coordinador es obligatorio.");
        }
        return builder.build();
    }
}
