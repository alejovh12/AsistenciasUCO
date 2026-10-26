package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Activa Keycloak únicamente para el selector legacy y su alias histórico. */
public final class LegacySecurityProviderCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String provider = context.getEnvironment()
                .getProperty("app.adapters.security.provider", "legacy");
        return "legacy".equalsIgnoreCase(provider) || "keycloak".equalsIgnoreCase(provider);
    }
}
