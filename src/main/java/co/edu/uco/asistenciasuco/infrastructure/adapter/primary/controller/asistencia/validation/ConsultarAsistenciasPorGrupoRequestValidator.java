package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

public final class ConsultarAsistenciasPorGrupoRequestValidator implements Validator<ConsultarAsistenciasPorGrupoRequest> {

    @Override
    public ValidationResult validate(final ConsultarAsistenciasPorGrupoRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion de consulta de asistencias es obligatoria.").build();
        }
        UuidFieldValidationHelper.validateRequiredUuid(builder, "grupo", request.getGrupo(), "El grupo es obligatorio.", "El identificador del grupo no es valido.");
        UuidFieldValidationHelper.validateOptionalUuid(builder, "sesion", request.getSesion(), "El identificador de la sesion no es valido.");
        return builder.build();
    }
}
