package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.List;

/**
 * Condicion para activar los beans concretos del outbox cuando
 * el provider es OUTBOX o cuando COMPOSITE incluye a OUTBOX entre sus destinos.
 */
public class OutboxAdapterCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String provider = context.getEnvironment().getProperty("app.adapters.notification.provider", "log").trim().toLowerCase();
        if ("outbox".equals(provider)) {
            return true;
        }
        if ("composite".equals(provider)) {
            final String targets = context.getEnvironment().getProperty("app.adapters.notification.composite.targets", "");
            final List<String> targetList = Arrays.stream(targets.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            return targetList.contains("outbox");
        }
        return false;
    }
}
