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
import java.util.UUID;

/**
 * ROL-06: el UUID nulo ({@code 00000000-0000-0000-0000-000000000000}) no identifica nada. Misma regla que
 * {@code ValidationHelper.isNonEmptyUuid} de los validators propios; {@code null} es valido (la obligatoriedad la declara
 * {@code @NotNull}). Por defecto se reporta como {@code FIELD_INVALID_UUID}; con
 * {@code payload = BeanValidationCodes.Required.class} como {@code FIELD_REQUIRED}, igual que el validator propio.
 */
@Documented
@Constraint(validatedBy = NonEmptyUuid.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonEmptyUuid {

    String message() default "El identificador no es valido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Implementacion de la restriccion. */
    final class Validator implements ConstraintValidator<NonEmptyUuid, UUID> {
        @Override
        public boolean isValid(final UUID value, final ConstraintValidatorContext context) {
            return ValidationHelper.isNonEmptyUuid(value);
        }
    }
}
