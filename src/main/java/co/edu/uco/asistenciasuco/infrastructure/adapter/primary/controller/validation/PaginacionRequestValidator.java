package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;

/**
 * Validacion comun de {@code pagina} y {@code tamano} de los listados paginados (CULM-001 D.9-a, CONTRACT D §11.1, F-ADM-023 y
 * F-EST-022): {@code pagina >= 1} y {@code 1 <= tamano <= PAGINACION/TAMANO_MAXIMO} (100, el mismo limite que aplica el SP).
 * Un valor fuera de rango responde 400 con el codigo de catalogo {@code PAG_001} y un {@code details[]} por campo; asi la
 * pagina nunca se acota en silencio y un cliente no puede volcar la tabla con {@code tamano=100000}.
 */
public final class PaginacionRequestValidator {

    /** Codigo de catalogo del rechazo (mensaje institucional existente de la BD). */
    public static final String CODIGO = "PAG_001";

    /** Pagina ya validada y con los valores por defecto aplicados. */
    public record Pagina(int numero, int tamano) {
    }

    private PaginacionRequestValidator() {
    }

    /** Valida y devuelve la pagina efectiva; lanza {@code RequestValidationException} (400 {@code PAG_001}) si no es valida. */
    public static Pagina resolver(final Integer pagina, final Integer tamano) {
        RequestValidationGuard.validate(validate(pagina, tamano), CODIGO);
        return new Pagina(
                pagina == null ? ConsultaPaginadaRules.PAGINA_POR_DEFECTO : pagina,
                tamano == null ? ConsultaPaginadaRules.TAMANO_POR_DEFECTO : tamano);
    }

    public static ValidationResult validate(final Integer pagina, final Integer tamano) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        builder.addIf(pagina != null && pagina < 1, "pagina", ValidationErrorType.OUT_OF_RANGE,
                "La pagina debe ser mayor o igual a 1.");
        builder.addIf(tamano != null && (tamano < 1 || tamano > ConsultaPaginadaRules.TAMANO_MAXIMO), "tamano",
                ValidationErrorType.OUT_OF_RANGE,
                "El tamano de pagina debe estar entre 1 y " + ConsultaPaginadaRules.TAMANO_MAXIMO + ".");
        return builder.build();
    }
}
