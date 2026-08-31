package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.AsignarDocenteAGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class AsignarDocenteAGrupoRequestValidator implements Validator<AsignarDocenteAGrupoRequest> {

    @Override
    public ValidationResult validate(final AsignarDocenteAGrupoRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de asignacion del docente es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "docente",
                request.getDocente(),
                "El docente es obligatorio.",
                "El identificador del docente no es valido."
        );
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "grupo",
                request.getGrupo(),
                "El grupo es obligatorio.",
                "El identificador del grupo no es valido."
        );
        return builder.build();
    }
}
