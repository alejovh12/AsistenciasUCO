package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorregirIdentidadRequest;

/**
 * Validacion de forma de la correccion de identidad (HU159, F-ADM-S12): primer nombre y primer apellido obligatorios,
 * longitudes y caracteres de nombres con mensajes amigables al usuario (sin exponer nombres tecnicos de campo en camelCase),
 * documento opcional y motivo obligatorio. Jerarquia, unicidad e interruptor del documento los decide la base de datos.
 */
public final class CorregirIdentidadRequestValidator implements Validator<CorregirIdentidadRequest> {

    private static final int MAX_NOMBRE = 50;

    @Override
    public ValidationResult validate(final CorregirIdentidadRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos de identidad son obligatorios.").build();
        }
        nombre(builder, "primerNombre", request.getPrimerNombre(), true);
        nombre(builder, "primerApellido", request.getPrimerApellido(), true);
        nombre(builder, "segundoNombre", request.getSegundoNombre(), false);
        nombre(builder, "segundoApellido", request.getSegundoApellido(), false);
        CorreccionDocumentoValidation.validar(builder, request, true);
        return builder.build();
    }

    private static void nombre(final ValidationResultBuilder builder, final String campo, final String valor,
                               final boolean obligatorio) {
        final String etiqueta = etiquetaCampo(campo);
        if (obligatorio && !ValidationHelper.hasText(valor)) {
            builder.add(campo, ValidationErrorType.REQUIRED, "El " + etiqueta + " es obligatorio.");
            return;
        }
        builder.addIf(valor != null && valor.trim().length() > MAX_NOMBRE, campo, ValidationErrorType.INVALID_LENGTH,
                "El " + etiqueta + " no puede superar " + MAX_NOMBRE + " caracteres.");
        builder.addIf(!ValidationHelper.isValidPersonName(valor), campo, ValidationErrorType.INVALID_FORMAT,
                "El " + etiqueta + " contiene caracteres no permitidos.");
    }

    private static String etiquetaCampo(final String campo) {
        return switch (campo) {
            case "primerNombre" -> "primer nombre";
            case "primerApellido" -> "primer apellido";
            case "segundoNombre" -> "segundo nombre";
            case "segundoApellido" -> "segundo apellido";
            default -> campo;
        };
    }
}
