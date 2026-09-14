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
        UuidFieldValidationHelper.validateRequiredUuid(builder, "sesionId", request.getSesionId(), "La sesion es obligatoria.", "El identificador de la sesion no es valido.");
        if (!ValidationHelper.hasText(request.getCategoria())) {
            builder.add("categoria", ValidationErrorType.REQUIRED, "La categoria es obligatoria.");
        }
        if (!ValidationHelper.hasText(request.getJustificacion())) {
            builder.add("justificacion", ValidationErrorType.REQUIRED, "La justificacion es obligatoria.");
        } else {
            builder.addIf(
                    !ValidationHelper.isLengthBetween(request.getJustificacion().trim(), 1, 300),
                    "justificacion",
                    ValidationErrorType.INVALID_LENGTH,
                    "La justificacion debe tener entre 1 y 300 caracteres."
            );
        }
        return builder.build();
    }
}
