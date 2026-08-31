package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarDocentePorIdRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class ConsultarDocentePorIdRequestValidator implements Validator<ConsultarDocentePorIdRequest> {

    @Override
    public ValidationResult validate(final ConsultarDocentePorIdRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de consulta del docente es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "docente",
                request.getDocente(),
                "El docente es obligatorio.",
                "El identificador del docente no es valido."
        );
        return builder.build();
    }
}
