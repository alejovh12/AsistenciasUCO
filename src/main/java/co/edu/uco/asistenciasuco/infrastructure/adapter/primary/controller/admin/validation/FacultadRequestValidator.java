package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.ActualizarFacultadRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.CrearFacultadRequest;

/**
 * Validacion de forma de {@code POST} y {@code PUT /api/v1/admin/facultades} (CULM-001 D.9-a, F-ADM-025): el nombre debe tener
 * entre 3 y 50 caracteres (la regla de los casos de uso), el decano del alta es obligatorio y la actualizacion exige al menos
 * un campo. Cada incumplimiento llega como un {@code details[]} con el campo y la regla; los mensajes no incluyen valores.
 */
public final class FacultadRequestValidator {

    private static final int MIN_NOMBRE = 3;
    private static final int MAX_NOMBRE = 50;

    public ValidationResult validarCreacion(final CrearFacultadRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos de la facultad son obligatorios.").build();
        }
        if (!ValidationHelper.hasText(request.nombre())) {
            builder.add("nombre", ValidationErrorType.REQUIRED, "El nombre de la facultad es obligatorio.");
        } else {
            nombre(builder, request.nombre());
        }
        builder.addIf(request.idDecano() == null || !ValidationHelper.isNonEmptyUuid(request.idDecano()), "idDecano", ValidationErrorType.REQUIRED,
                "El decano titular de la facultad es obligatorio.");
        return builder.build();
    }

    public ValidationResult validarActualizacion(final ActualizarFacultadRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos de la facultad son obligatorios.").build();
        }
        final boolean sinNombre = !ValidationHelper.hasText(request.nombre());
        if (sinNombre && request.idDecano() == null) {
            return builder.add("nombre", ValidationErrorType.REQUIRED, "Debe indicar al menos el nombre o el decano de la facultad.").build();
        }
        if (!sinNombre) {
            nombre(builder, request.nombre());
        }
        return builder.build();
    }

    private static void nombre(final ValidationResultBuilder builder, final String valor) {
        final int longitud = valor.trim().length();
        builder.addIf(longitud < MIN_NOMBRE || longitud > MAX_NOMBRE, "nombre", ValidationErrorType.INVALID_LENGTH,
                "El nombre de la facultad debe tener entre " + MIN_NOMBRE + " y " + MAX_NOMBRE + " caracteres.");
    }
}
