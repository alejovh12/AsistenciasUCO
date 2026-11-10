package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.AprobarSolicitudSesionRequest;

/**
 * DEC-065 (2): forma del cuerpo de la aprobacion. El cuerpo y la observacion son opcionales (hasta 500 caracteres); el maximo efectivo
 * ({@code SES_021}) lo valida la DB.
 */
public final class AprobarSolicitudSesionRequestValidator implements Validator<AprobarSolicitudSesionRequest> {

    public static final int LONGITUD_MAXIMA_OBSERVACION = 500;

    @Override
    public ValidationResult validate(final AprobarSolicitudSesionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        builder.addIf(request != null && request.getObservacion() != null
                        && request.getObservacion().strip().length() > LONGITUD_MAXIMA_OBSERVACION,
                "observacion", ValidationErrorType.INVALID_LENGTH, "La observacion no puede superar 500 caracteres.");
        return builder.build();
    }
}
