package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Activa Keycloak únicamente si el selector es legacy (o su alias histórico) de forma explícita; ausente no activa nada. */
public final class LegacySecurityProviderCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String provider = context.getEnvironment()
                .getProperty("app.adapters.security.provider", "");
        return "legacy".equalsIgnoreCase(provider) || "keycloak".equalsIgnoreCase(provider);
    }
}
