package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CambiarEstadoDocenteProgramaRequest;

public final class CambiarEstadoDocenteProgramaRequestValidator implements Validator<CambiarEstadoDocenteProgramaRequest> {

    @Override
    public ValidationResult validate(final CambiarEstadoDocenteProgramaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion del estado es obligatoria.").build();
        }
        if (request.getEstaActivo() == null) {
            builder.add("estaActivo", ValidationErrorType.REQUIRED, "El estado (estaActivo) es obligatorio.");
        }
        return builder.build();
    }
}
