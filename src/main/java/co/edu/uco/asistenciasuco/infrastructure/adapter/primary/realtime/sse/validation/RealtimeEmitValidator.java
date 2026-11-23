package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valida el cuerpo de {@code POST /api/v1/realtime/emit} (CULM-001 D.9-c, F-TRX-016): {@code tipo} del catalogo de eventos,
 * {@code audiencia} autorizada por la politica para ese tipo y {@code payload} objeto JSON. Cualquier incumplimiento responde
 * 400 {@code VALIDATION_ERROR}. Se saco del controlador para que este solo adapte HTTP.
 */
public final class RealtimeEmitValidator {

    private static final String CLAVE_TIPO = "tipo";
    private static final String CLAVE_AUDIENCIA = "audiencia";
    private static final String CLAVE_PAYLOAD = "payload";
    private static final RealtimeAudienciaPolicy POLITICA = RealtimeAudienciaPolicy.porDefecto();

    private RealtimeEmitValidator() {
    }

    /** Evento ya validado y listo para emitir. */
    public record EventoEmitido(String tipo, Map<String, Object> payload) {
    }

    public static EventoEmitido validar(final Map<String, Object> cuerpo) {
        final List<ValidationIssue> issues = new ArrayList<>();
        final Map<String, Object> body = cuerpo == null ? Map.of() : cuerpo;
        final String tipo = validarTipo(body.get(CLAVE_TIPO), issues);
        validarAudiencia(tipo, body.get(CLAVE_AUDIENCIA), issues);
        final Map<String, Object> payload = validarPayload(body.get(CLAVE_PAYLOAD), issues);
        RequestValidationGuard.validate(ValidationResult.of(issues));
        return new EventoEmitido(tipo, payload);
    }

    static String validarTipo(final Object valor, final List<ValidationIssue> issues) {
        if (valor == null) {
            issues.add(new ValidationIssue("tipo", ValidationErrorType.REQUIRED, "El tipo de evento es obligatorio."));
            return null;
        }
        if (!(valor instanceof String tipo)) {
            issues.add(new ValidationIssue("tipo", ValidationErrorType.INVALID_TYPE, "El tipo de evento debe ser texto."));
            return null;
        }
        if (!POLITICA.tiposConocidos().contains(tipo)) {
            issues.add(new ValidationIssue("tipo", ValidationErrorType.INVALID_VALUE, "El tipo de evento no pertenece al catálogo de eventos."));
            return null;
        }
        return tipo;
    }

    static void validarAudiencia(final String tipo, final Object valor, final List<ValidationIssue> issues) {
        if (valor == null) {
            issues.add(new ValidationIssue(CLAVE_AUDIENCIA, ValidationErrorType.REQUIRED, "La audiencia es obligatoria."));
            return;
        }
        if (!(valor instanceof String texto)) {
            issues.add(new ValidationIssue(CLAVE_AUDIENCIA, ValidationErrorType.INVALID_TYPE, "La audiencia debe ser texto."));
            return;
        }
        final RealtimeAudienciaPolicy.Audiencia audiencia = Arrays.stream(RealtimeAudienciaPolicy.Audiencia.values())
                .filter(candidata -> candidata.name().equals(texto))
                .findFirst()
                .orElse(null);
        if (audiencia == null) {
            issues.add(new ValidationIssue(CLAVE_AUDIENCIA, ValidationErrorType.INVALID_VALUE, "La audiencia no existe."));
        } else if (tipo != null && !POLITICA.entregaA(tipo, audiencia)) {
            issues.add(new ValidationIssue(CLAVE_AUDIENCIA, ValidationErrorType.INVALID_VALUE,
                    "La política de audiencia del tipo de evento no autoriza esa audiencia."));
        }
    }

    static Map<String, Object> validarPayload(final Object valor, final List<ValidationIssue> issues) {
        if (valor == null) {
            issues.add(new ValidationIssue(CLAVE_PAYLOAD, ValidationErrorType.REQUIRED, "El payload es obligatorio."));
            return Map.of();
        }
        if (!(valor instanceof Map<?, ?> mapa)) {
            issues.add(new ValidationIssue(CLAVE_PAYLOAD, ValidationErrorType.INVALID_TYPE, "El payload debe ser un objeto JSON."));
            return Map.of();
        }
        final Map<String, Object> payload = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : mapa.entrySet()) {
            if (!(entry.getKey() instanceof String clave)) {
                issues.add(new ValidationIssue(CLAVE_PAYLOAD, ValidationErrorType.INVALID_TYPE, "Las claves del payload deben ser texto."));
                return Map.of();
            }
            payload.put(clave, entry.getValue());
        }
        return Map.copyOf(payload);
    }
}
