package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.request.DecisionRevisionCancelacionRequest;

/** CULM-001 C.1 ([SUPUESTO-C-19]): la decision no es vacia y el motivo existe; los valores y la longitud los valida la DB (RCN_002, RCN_003). */
public final class DecisionRevisionCancelacionRequestValidator implements Validator<DecisionRevisionCancelacionRequest> {

    @Override
    public ValidationResult validate(final DecisionRevisionCancelacionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("decision", ValidationErrorType.REQUIRED, "La decision es obligatoria.").build();
        }
        return builder
                .addIf(!ValidationHelper.hasText(request.getDecision()), "decision", ValidationErrorType.REQUIRED, "La decision es obligatoria.")
                .addIf(request.getMotivo() == null, "motivo", ValidationErrorType.REQUIRED, "El motivo es obligatorio.")
                .build();
    }
}
