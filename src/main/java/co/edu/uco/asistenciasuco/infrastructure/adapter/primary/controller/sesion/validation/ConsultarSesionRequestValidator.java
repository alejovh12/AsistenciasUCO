package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class ConsultarSesionRequestValidator implements Validator<ConsultarSesionRequest> {

    @Override
    public ValidationResult validate(final ConsultarSesionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de consulta de sesion es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "sesion",
                request.getSesion(),
                "La sesion es obligatoria.",
                "El identificador de la sesion no es valido."
        );
        return builder.build();
    }
}
