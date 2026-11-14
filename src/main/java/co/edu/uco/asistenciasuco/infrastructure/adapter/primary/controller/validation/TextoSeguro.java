package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nombre o codigo de una entidad academica (DEC-073): letras, digitos, espacio y {@code . , - _ ( ) / # :}; sin {@code < >}.
 * {@code null} y el texto vacio son validos: la obligatoriedad la declara {@code @NotBlank}. Se reporta como
 * {@code FIELD_INVALID_FORMAT}.
 */
@Documented
@Constraint(validatedBy = TextoSeguro.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TextoSeguro {

    String message() default "El texto solo admite letras, numeros, espacios y los signos . , - _ ( ) / # :.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Implementacion de la restriccion. */
    final class Validator implements ConstraintValidator<TextoSeguro, String> {
        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            return ValidationHelper.isSafeText(value);
        }
    }
}
