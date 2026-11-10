package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController.CrearDecanoRequest;

/**
 * Validacion de forma de {@code POST /api/v1/admin/decanos} (CULM-001 D.9-a, F-ADM-025): cada incumplimiento llega al cliente como
 * un {@code details[]} con el campo y la regla. La politica de la clave inicial NO se evalua aqui: vive solo en el backend
 * (PasswordRegistroRule) y el cliente no la duplica; la unicidad y el correo institucional los decide el caso de uso. Los mensajes
 * nunca incluyen los valores recibidos.
 */
public final class CrearDecanoRequestValidator implements Validator<CrearDecanoRequest> {

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_CORREO = 255;
    private static final int MIN_NOMBRE_FACULTAD = 3;
    private static final int MAX_NOMBRE_FACULTAD = 50;
    private static final int MAX_CODIGO_FACULTAD = 20;

    @Override
    public ValidationResult validate(final CrearDecanoRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos del decano son obligatorios.").build();
        }
        builder.addIf(request.tipoIdentificacionId() == null || !ValidationHelper.isNonEmptyUuid(request.tipoIdentificacionId()),
                "tipoIdentificacionId", ValidationErrorType.REQUIRED, "El tipo de identificacion es obligatorio.");
        builder.addIf(request.numeroIdentificacion() == null || !ValidationHelper.isPositive(request.numeroIdentificacion()),
                "numeroIdentificacion", request.numeroIdentificacion() == null ? ValidationErrorType.REQUIRED : ValidationErrorType.INVALID_FORMAT,
                "El numero de identificacion es obligatorio y debe ser positivo.");
        nombre(builder, "primerNombre", request.primerNombre(), true);
        nombre(builder, "primerApellido", request.primerApellido(), true);
        nombre(builder, "segundoNombre", request.segundoNombre(), false);
        nombre(builder, "segundoApellido", request.segundoApellido(), false);
        if (!ValidationHelper.hasText(request.correo())) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
        } else {
            builder.addIf(request.correo().trim().length() > MAX_CORREO, "correo", ValidationErrorType.INVALID_LENGTH,
                    "El correo no puede superar " + MAX_CORREO + " caracteres.");
            builder.addIf(!ValidationHelper.isValidEmail(request.correo().trim()), "correo", ValidationErrorType.INVALID_FORMAT,
                    "El correo no tiene un formato valido.");
        }
        facultadNueva(builder, request);
        return builder.build();
    }

    /** DEC-065 (5): la facultad es existente o nueva (nunca ambas); la nueva exige nombre de 3 a 50 caracteres e institucion. */
    private static void facultadNueva(final ValidationResultBuilder builder, final CrearDecanoRequest request) {
        final var facultad = request.facultadNueva();
        if (facultad == null) {
            return;
        }
        builder.addIf(request.idFacultad() != null, "facultadNueva", ValidationErrorType.INVALID_VALUE,
                "Indique una facultad existente o una facultad nueva, no ambas.");
        builder.addIf(!ValidationHelper.hasText(facultad.nombre())
                        || !ValidationHelper.isLengthBetween(facultad.nombre().trim(), MIN_NOMBRE_FACULTAD, MAX_NOMBRE_FACULTAD),
                "facultadNueva.nombre", ValidationErrorType.INVALID_LENGTH,
                "El nombre de la facultad debe tener entre " + MIN_NOMBRE_FACULTAD + " y " + MAX_NOMBRE_FACULTAD + " caracteres.");
        builder.addIf(ValidationHelper.hasText(facultad.codigo()) && facultad.codigo().trim().length() > MAX_CODIGO_FACULTAD,
                "facultadNueva.codigo", ValidationErrorType.INVALID_LENGTH,
                "El codigo de la facultad no puede superar " + MAX_CODIGO_FACULTAD + " caracteres.");
        builder.addIf(facultad.idInstitucion() == null || !ValidationHelper.isNonEmptyUuid(facultad.idInstitucion()),
                "facultadNueva.idInstitucion", ValidationErrorType.REQUIRED, "La institucion de la facultad es obligatoria.");
    }

    private static void nombre(final ValidationResultBuilder builder, final String campo, final String valor, final boolean obligatorio) {
        if (obligatorio && !ValidationHelper.hasText(valor)) {
            builder.add(campo, ValidationErrorType.REQUIRED, "El campo " + campo + " es obligatorio.");
            return;
        }
        builder.addIf(valor != null && valor.trim().length() > MAX_NOMBRE, campo, ValidationErrorType.INVALID_LENGTH,
                "El campo " + campo + " no puede superar " + MAX_NOMBRE + " caracteres.");
        builder.addIf(ValidationHelper.hasText(valor) && !ValidationHelper.isValidPersonName(valor), campo, ValidationErrorType.INVALID_FORMAT,
                "El campo " + campo + " contiene caracteres no permitidos.");
    }
}
