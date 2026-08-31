package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JacksonInputErrorResolver {

    private JacksonInputErrorResolver() {
    }

    public static Optional<ResolvedInputError> resolve(final HttpMessageNotReadableException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
                return resolveUnrecognizedPropertyException(unrecognizedPropertyException);
            }
            if (current instanceof InputCoercionException inputCoercionException) {
                return resolveCoercionException(inputCoercionException);
            }
            if (current instanceof InvalidFormatException invalidFormatException) {
                return resolveMappingException(invalidFormatException, invalidFormatException.getTargetType());
            }
            if (current instanceof MismatchedInputException mismatchedInputException) {
                return resolveMappingException(mismatchedInputException, mismatchedInputException.getTargetType());
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static Optional<ResolvedInputError> resolveUnrecognizedPropertyException(
            final UnrecognizedPropertyException exception
    ) {
        final Optional<String> field = HttpFieldNameSanitizer.sanitize(exception.getPropertyName());
        if (field.isEmpty()) {
            return Optional.empty();
        }
        final String sanitizedField = field.get();
        return Optional.of(new ResolvedInputError(
                sanitizedField,
                ApiFieldErrorCode.FIELD_UNKNOWN,
                "El campo '" + sanitizedField + "' no forma parte del contrato de esta solicitud."
        ));
    }

    private static Optional<ResolvedInputError> resolveCoercionException(final InputCoercionException exception) {
        final Optional<String> resolvedField = resolveField(exception);
        final Class<?> targetType = exception.getTargetType();
        if (resolvedField.isEmpty() || targetType == null) {
            return Optional.empty();
        }
        final String field = resolvedField.get();
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Optional.of(new ResolvedInputError(
                    field,
                    ApiFieldErrorCode.FIELD_OUT_OF_RANGE,
                    integerOutOfRangeMessage(field)
            ));
        }
        return Optional.of(new ResolvedInputError(
                field,
                ApiFieldErrorCode.FIELD_OUT_OF_RANGE,
                "El valor numerico enviado excede el rango permitido para este campo."
        ));
    }

    private static Optional<ResolvedInputError> resolveMappingException(
            final JacksonException exception,
            final Class<?> targetType
    ) {
        final Optional<String> resolvedField = resolveField(exception);
        if (resolvedField.isEmpty() || targetType == null) {
            return Optional.empty();
        }
        final String field = resolvedField.get();
        if (UUID.class.equals(targetType)) {
            return Optional.of(new ResolvedInputError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_UUID,
                    "El identificador enviado no tiene un formato UUID valido."
            ));
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Optional.of(new ResolvedInputError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_TYPE,
                    integerInvalidTypeMessage(field)
            ));
        }
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            return Optional.of(new ResolvedInputError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_TYPE,
                    "El campo debe contener true o false."
            ));
        }
        return Optional.of(new ResolvedInputError(
                field,
                ApiFieldErrorCode.FIELD_INVALID_TYPE,
                "El tipo de dato enviado para este campo no es valido."
        ));
    }

    private static Optional<String> resolveField(final JacksonException exception) {
        final List<JacksonException.Reference> path = exception.getPath();
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        for (int index = path.size() - 1; index >= 0; index--) {
            final Optional<String> propertyName = HttpFieldNameSanitizer.sanitize(path.get(index).getPropertyName());
            if (propertyName.isPresent()) {
                return propertyName;
            }
        }
        return Optional.empty();
    }

    private static String integerInvalidTypeMessage(final String field) {
        if ("numeroIdentificacion".equals(field)) {
            return "El numero de identificacion debe contener un numero entero valido.";
        }
        return "El tipo de dato enviado para este campo no es valido.";
    }

    private static String integerOutOfRangeMessage(final String field) {
        if ("numeroIdentificacion".equals(field)) {
            return "El numero de identificacion excede el rango numerico permitido por el contrato actual.";
        }
        return "El valor numerico enviado excede el rango permitido para este campo.";
    }
}
