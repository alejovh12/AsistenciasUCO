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
 * Numero de identificacion que llega como texto (DEC-073): solo digitos, sin cero inicial y dentro del rango de un INT de SQL Server.
 * {@code null} y el texto vacio son validos: la obligatoriedad la declara {@code @NotBlank}. Se reporta como
 * {@code FIELD_INVALID_FORMAT}.
 */
@Documented
@Constraint(validatedBy = NumeroIdentificacionTexto.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NumeroIdentificacionTexto {

    String message() default "El numero de identificacion solo admite digitos (hasta 10) y debe ser positivo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Implementacion de la restriccion. */
    final class Validator implements ConstraintValidator<NumeroIdentificacionTexto, String> {
        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            return ValidationHelper.isIdentificationNumberText(value);
        }
    }
}
