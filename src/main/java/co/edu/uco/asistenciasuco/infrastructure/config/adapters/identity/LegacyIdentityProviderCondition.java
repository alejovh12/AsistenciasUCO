package co.edu.uco.asistenciasuco.infrastructure.config.adapters.identity;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activa el aprovisionamiento sobre el Keycloak legacy únicamente para el selector legacy y su alias histórico
 * {@code keycloak}. Un valor ausente equivale a legacy; un valor desconocido no activa nada (y además falla el
 * binding de {@code IdentityAdapterProperties}: no hay degradación silenciosa).
 */
public final class LegacyIdentityProviderCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String provider = context.getEnvironment().getProperty("app.adapters.identity.provider", "legacy");
        return "legacy".equalsIgnoreCase(provider) || "keycloak".equalsIgnoreCase(provider);
    }
}
