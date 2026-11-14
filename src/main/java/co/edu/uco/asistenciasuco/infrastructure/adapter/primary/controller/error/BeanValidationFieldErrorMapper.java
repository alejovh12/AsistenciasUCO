package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationCodes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ROL-06: traduce las violaciones de Jakarta Bean Validation al contrato {@code details[]} que ya usan los validators
 * propios ({@link ApiFieldError} con {@link ApiFieldErrorCode}).
 *
 * <ul>
 *   <li>Un error por campo, con prioridad fija: obligatorio, formato, longitud, rango, valor.</li>
 *   <li>Orden estable: el orden de declaracion de los campos en el request (no el orden aleatorio del proveedor).</li>
 *   <li>Solo se expone el {@code message} explicito de la anotacion; el texto por defecto del proveedor (plantilla
 *       {@code {jakarta.validation...}}, localizado y en ingles) se sustituye por un mensaje controlado.</li>
 * </ul>
 */
public final class BeanValidationFieldErrorMapper {

    private BeanValidationFieldErrorMapper() {
    }

    public static List<ApiFieldError> from(final BindingResult bindingResult) {
        if (bindingResult == null) {
            return List.of();
        }
        final Map<String, Candidate> porCampo = new LinkedHashMap<>();
        for (final FieldError fieldError : bindingResult.getFieldErrors()) {
            final Candidate candidate = toCandidate(fieldError);
            porCampo.merge(candidate.error().field(), candidate,
                    (actual, nuevo) -> nuevo.prioridad() < actual.prioridad() ? nuevo : actual);
        }
        final List<String> orden = declaredFieldOrder(bindingResult.getTarget());
        final List<ApiFieldError> resultado = new ArrayList<>(porCampo.values().stream()
                .sorted(Comparator.comparingInt((Candidate c) -> indexOf(orden, c.error().field()))
                        .thenComparing(c -> c.error().field()))
                .map(Candidate::error)
                .toList());
        for (final ObjectError globalError : bindingResult.getGlobalErrors()) {
            resultado.add(new ApiFieldError("request", ApiFieldErrorCode.FIELD_INVALID_VALUE.name(), safeMessage(globalError,
                    "La informacion enviada no es valida.")));
        }
        return List.copyOf(resultado);
    }

    private static Candidate toCandidate(final FieldError fieldError) {
        final String field = fieldError.getField();
        final ApiFieldErrorCode code = payloadCode(fieldError).orElseGet(() -> toApiCode(fieldError.getCode()));
        final String message = safeMessage(fieldError, defaultMessage(code, field));
        return new Candidate(new ApiFieldError(field, code.name(), message), code.ordinal());
    }

    /** {@code payload} de {@link BeanValidationCodes}: el request fija el codigo que ya usa su validator propio. */
    private static Optional<ApiFieldErrorCode> payloadCode(final FieldError fieldError) {
        if (!fieldError.contains(ConstraintViolation.class)) {
            return Optional.empty();
        }
        final Set<Class<? extends Payload>> payload =
                fieldError.unwrap(ConstraintViolation.class).getConstraintDescriptor().getPayload();
        if (payload.contains(BeanValidationCodes.Required.class)) {
            return Optional.of(ApiFieldErrorCode.FIELD_REQUIRED);
        }
        if (payload.contains(BeanValidationCodes.InvalidFormat.class)) {
            return Optional.of(ApiFieldErrorCode.FIELD_INVALID_FORMAT);
        }
        if (payload.contains(BeanValidationCodes.InvalidLength.class)) {
            return Optional.of(ApiFieldErrorCode.FIELD_INVALID_LENGTH);
        }
        if (payload.contains(BeanValidationCodes.OutOfRange.class)) {
            return Optional.of(ApiFieldErrorCode.FIELD_OUT_OF_RANGE);
        }
        return Optional.empty();
    }

    /** Prioridad = ordinal de {@link ApiFieldErrorCode}: REQUIRED antes que FORMAT, LENGTH y RANGE. */
    static ApiFieldErrorCode toApiCode(final String constraint) {
        if (constraint == null) {
            return ApiFieldErrorCode.FIELD_INVALID_VALUE;
        }
        return switch (constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> ApiFieldErrorCode.FIELD_REQUIRED;
            case "Size", "Length" -> ApiFieldErrorCode.FIELD_INVALID_LENGTH;
            case "Pattern", "Email", "NombrePersona", "TextoSeguro", "SinEtiquetasHtml", "NumeroIdentificacionTexto" ->
                    ApiFieldErrorCode.FIELD_INVALID_FORMAT;
            case "NonEmptyUuid" -> ApiFieldErrorCode.FIELD_INVALID_UUID;
            case "Min", "Max", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero", "DecimalMin", "DecimalMax",
                 "Digits", "Range" -> ApiFieldErrorCode.FIELD_OUT_OF_RANGE;
            default -> ApiFieldErrorCode.FIELD_INVALID_VALUE;
        };
    }

    private static String defaultMessage(final ApiFieldErrorCode code, final String field) {
        final String detalle = switch (code) {
            case FIELD_REQUIRED -> "es obligatorio.";
            case FIELD_INVALID_LENGTH -> "no cumple la longitud permitida.";
            case FIELD_INVALID_FORMAT -> "no tiene un formato valido.";
            case FIELD_OUT_OF_RANGE -> "esta fuera del rango permitido.";
            default -> "no es valido.";
        };
        return "El campo '" + field + "' " + detalle;
    }

    /** Solo el {@code message} explicito de la anotacion; nunca la plantilla por defecto del proveedor. */
    private static String safeMessage(final ObjectError error, final String fallback) {
        if (error.contains(ConstraintViolation.class)) {
            final ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
            final String template = violation.getMessageTemplate();
            if (template != null && !template.isBlank() && !template.contains("{")) {
                return template;
            }
        }
        return fallback;
    }

    private static List<String> declaredFieldOrder(final Object target) {
        if (target == null) {
            return List.of();
        }
        final List<String> orden = new ArrayList<>();
        final List<Class<?>> jerarquia = new ArrayList<>();
        for (Class<?> type = target.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            jerarquia.addFirst(type);
        }
        for (final Class<?> type : jerarquia) {
            for (final Field field : type.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    orden.add(field.getName());
                }
            }
        }
        return orden;
    }

    private static int indexOf(final List<String> orden, final String path) {
        final int punto = path.indexOf('.');
        final int corchete = path.indexOf('[');
        int fin = path.length();
        if (punto >= 0) {
            fin = Math.min(fin, punto);
        }
        if (corchete >= 0) {
            fin = Math.min(fin, corchete);
        }
        final int index = orden.indexOf(path.substring(0, fin));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private record Candidate(ApiFieldError error, int prioridad) {
    }
}
