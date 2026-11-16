package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;

/**
 * Validacion comun del parametro opcional {@code busqueda} de los listados que filtran por texto en el servidor
 * (CIERRE-W1A): despues de recortar, no puede superar {@value #LONGITUD_MAXIMA} caracteres (el parametro del SP es
 * {@code NVARCHAR(100)}); un texto mayor es 400 y no se trunca en silencio. Nulo o vacio significa sin filtro.
 */
public final class BusquedaRequestValidator {

    /** Longitud maxima de la busqueda tras recortar (igual que {@code @busqueda NVARCHAR(100)} de los SP). */
    public static final int LONGITUD_MAXIMA = 100;

    private BusquedaRequestValidator() {
    }

    public static ValidationResult validate(final String busqueda) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        builder.addIf(busqueda != null && busqueda.trim().length() > LONGITUD_MAXIMA, "busqueda",
                ValidationErrorType.INVALID_LENGTH,
                "La busqueda no puede superar " + LONGITUD_MAXIMA + " caracteres.");
        return builder.build();
    }
}
