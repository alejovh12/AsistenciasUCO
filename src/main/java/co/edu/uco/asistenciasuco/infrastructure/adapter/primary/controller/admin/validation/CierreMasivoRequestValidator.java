package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.CierreMasivoRequest;

/** Validacion de forma de {@code POST /api/v1/admin/cierre-masivo} (CULM-001 D, F-ADM-026): el periodo academico es obligatorio. */
public final class CierreMasivoRequestValidator implements Validator<CierreMasivoRequest> {

    @Override
    public ValidationResult validate(final CierreMasivoRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null || request.idPeriodoAcademico() == null || !ValidationHelper.isNonEmptyUuid(request.idPeriodoAcademico())) {
            builder.add("idPeriodoAcademico", ValidationErrorType.REQUIRED, "El periodo academico es obligatorio.");
        }
        return builder.build();
    }
}
