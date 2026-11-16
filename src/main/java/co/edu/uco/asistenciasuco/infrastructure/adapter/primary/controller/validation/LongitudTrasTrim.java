package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

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
 * Limita la longitud con la misma normalizacion usada por los validators HTTP propios. {@code null} es valido; la
 * obligatoriedad corresponde a {@code @NotBlank} o al validator propio.
 */
@Documented
@Constraint(validatedBy = LongitudTrasTrim.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface LongitudTrasTrim {

    int max();

    String message() default "El campo no puede superar {max} caracteres.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Implementacion de la restriccion sobre el texto ya recortado. */
    final class Validator implements ConstraintValidator<LongitudTrasTrim, String> {
        private int max;

        @Override
        public void initialize(final LongitudTrasTrim constraint) {
            max = constraint.max();
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            return value == null || value.trim().length() <= max;
        }
    }
}
