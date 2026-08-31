package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.RegistrarDocenteDesdeUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class RegistrarDocenteDesdeUsuarioRequestValidator implements Validator<RegistrarDocenteDesdeUsuarioRequest> {

    @Override
    public ValidationResult validate(final RegistrarDocenteDesdeUsuarioRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion del docente es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "usuario",
                request.getUsuario(),
                "El usuario es obligatorio.",
                "El identificador del usuario no es valido."
        );
        return builder.build();
    }
}
