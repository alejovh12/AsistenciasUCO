package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CancelarSesionCoordinadorRequest;

/**
 * CULM-001 / DEC-064 (HU047): forma del cuerpo. El codigo del motivo es obligatorio (hasta 50 caracteres) y la observacion es
 * opcional (hasta 500); el motivo del catalogo ({@code SES_019}) y el maximo efectivo de la observacion
 * ({@code SESION/OBSERVACION_CANCELACION_COORDINADOR_MAXIMA}, {@code SES_021}) los valida la DB.
 */
public final class CancelarSesionCoordinadorRequestValidator implements Validator<CancelarSesionCoordinadorRequest> {

    public static final int LONGITUD_MAXIMA_CODIGO_MOTIVO = 50;
    public static final int LONGITUD_MAXIMA_OBSERVACION = 500;

    @Override
    public ValidationResult validate(final CancelarSesionCoordinadorRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null || !ValidationHelper.hasText(request.getCodigoMotivo())) {
            return builder.add("codigoMotivo", ValidationErrorType.REQUIRED, "El motivo de cancelacion es obligatorio.").build();
        }
        builder.addIf(request.getCodigoMotivo().strip().length() > LONGITUD_MAXIMA_CODIGO_MOTIVO, "codigoMotivo",
                ValidationErrorType.INVALID_LENGTH, "El codigo del motivo no puede superar 50 caracteres.");
        builder.addIf(request.getObservacion() != null && request.getObservacion().strip().length() > LONGITUD_MAXIMA_OBSERVACION,
                "observacion", ValidationErrorType.INVALID_LENGTH, "La observacion no puede superar 500 caracteres.");
        return builder.build();
    }
}
