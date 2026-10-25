package co.edu.uco.asistenciasuco.infrastructure.config.sesion;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Puerta de activacion del contrato {@code /api/v2/sesiones} (MAINT-003F).
 *
 * <p>Unico lugar que interpreta {@code app.sesiones.v2.enabled}. Propiedad ausente o {@code true}:
 * V2 activo y el arranque exige que la base de datos del ambiente tenga desplegado UTC-D06-POST-FREEZE
 * ({@code uv_sesion_v2}, {@code usp_crear_sesion_v2}, {@code usp_actualizar_sesion_v2});
 * {@code SesionV2SchemaCompatibilityVerifier} lo comprueba y detiene el arranque si falta.
 * {@code false}: salida de compatibilidad explicita que responde 404
 * ({@link SesionesV2ActivationConfiguration}). Cualquier otro valor (vacio, {@code tru}, ...) es un
 * error de configuracion y detiene el arranque: nunca queda V2 accesible sin verificar el contrato SQL.</p>
 */
public final class SesionesV2Activation {

    public static final String PREFIX = "app.sesiones.v2";
    public static final String NAME = "enabled";
    public static final String PROPERTY = PREFIX + "." + NAME;

    private SesionesV2Activation() {
    }

    /**
     * Resuelve la activacion de forma estricta.
     *
     * @throws IllegalStateException si la propiedad existe con un valor distinto de {@code true}/{@code false}
     */
    public static boolean isEnabled(final PropertyResolver environment) {
        final String raw = environment.getProperty(PROPERTY);
        if (raw == null || "true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        throw new IllegalStateException(PROPERTY + " debe ser 'true', 'false' o estar ausente; valor recibido: '" + raw + "'.");
    }

    /** V2 activo: instala el verificador del contrato SQL. */
    public static final class Enabled implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return isEnabled(context.getEnvironment());
        }
    }

    /** V2 desactivado explicitamente: instala el bloqueo 404 de compatibilidad. */
    public static final class Disabled implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return !isEnabled(context.getEnvironment());
        }
    }
}
