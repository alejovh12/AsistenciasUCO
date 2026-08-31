package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class CerrarSesionRequestValidator implements Validator<CerrarSesionRequest> {

    @Override
    public ValidationResult validate(final CerrarSesionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de cierre de sesion es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "sesion",
                request.getSesion(),
                "La sesion es obligatoria.",
                "El identificador de la sesion no es valido."
        );
        if (!ValidationHelper.hasText(request.getObservacionCierre())) {
            builder.add("observacionCierre", ValidationErrorType.REQUIRED, "La observacion de cierre es obligatoria.");
        } else {
            builder.addIf(
                    !ValidationHelper.isLengthBetween(request.getObservacionCierre().trim(), 10, 250),
                    "observacionCierre",
                    ValidationErrorType.INVALID_LENGTH,
                    "La observacion de cierre debe tener entre 10 y 250 caracteres."
            );
        }
        return builder.build();
    }
}
