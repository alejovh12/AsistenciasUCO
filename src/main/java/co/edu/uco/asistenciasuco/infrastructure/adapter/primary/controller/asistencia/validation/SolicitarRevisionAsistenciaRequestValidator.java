package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class SolicitarRevisionAsistenciaRequestValidator implements Validator<SolicitarRevisionAsistenciaRequest> {

    @Override
    public ValidationResult validate(final SolicitarRevisionAsistenciaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de revision de asistencia es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(builder, "asistencia", request.getAsistencia(), "La asistencia es obligatoria.", "El identificador de la asistencia no es valido.");
        if (!ValidationHelper.hasText(request.getMotivo())) {
            builder.add("motivo", ValidationErrorType.REQUIRED, "El motivo es obligatorio.");
        } else {
            builder.addIf(
                    !ValidationHelper.isLengthBetween(request.getMotivo().trim(), 10, 300),
                    "motivo",
                    ValidationErrorType.INVALID_LENGTH,
                    "El motivo debe tener entre 10 y 300 caracteres."
            );
        }
        return builder.build();
    }
}
