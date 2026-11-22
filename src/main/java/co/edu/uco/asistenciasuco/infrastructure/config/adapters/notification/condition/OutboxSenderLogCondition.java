package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Complemento de {@link OutboxSenderServicioCondition}: el canal de salida del outbox es el de log. */
public class OutboxSenderLogCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        return !OutboxSenderServicioCondition.esServicio(context.getEnvironment());
    }
}
