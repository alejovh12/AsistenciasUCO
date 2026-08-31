package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation;

import co.edu.uco.asistenciasuco.crosscutting.helpers.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class RegistrarAsistenciaRequestValidator implements Validator<RegistrarAsistenciaRequest> {

    @Override
    public ValidationResult validate(final RegistrarAsistenciaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de asistencia es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(builder, "estudiante", request.getEstudiante(), "El estudiante es obligatorio.", "El identificador del estudiante no es valido.");
        UuidFieldValidationHelper.validateRequiredUuid(builder, "grupo", request.getGrupo(), "El grupo es obligatorio.", "El identificador del grupo no es valido.");
        UuidFieldValidationHelper.validateRequiredUuid(builder, "sesion", request.getSesion(), "La sesion es obligatoria.", "El identificador de la sesion no es valido.");
        builder.addIf(
                request.getPresente() == null,
                "presente",
                ValidationErrorType.REQUIRED,
                "La marca de asistencia es obligatoria."
        );
        if (ValidationHelper.hasText(request.getObservacion())) {
            builder.addIf(
                    !ValidationHelper.isLengthAtMost(request.getObservacion().trim(), 250),
                    "observacion",
                    ValidationErrorType.INVALID_LENGTH,
                    "La observacion puede tener maximo 250 caracteres."
            );
        }
        return builder.build();
    }
}
