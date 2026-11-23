package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition;

import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * El canal de salida del outbox es el Servicio de Notificaciones cuando {@code app.providers.outbox-notification.sender} es
 * {@code servicio} (o el alias heredado {@code smtp}) o, si no se definio, cuando hay una URL en
 * {@code asistencias.notificaciones.service.url}. Con {@code log} explicito, o sin sender ni URL (pruebas y desarrollo
 * aislado), el canal es el de log.
 */
public class OutboxSenderServicioCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        return esServicio(context.getEnvironment());
    }

    public static boolean esServicio(final Environment entorno) {
        final String sender = entorno.getProperty("app.providers.outbox-notification.sender", "").trim().toLowerCase();
        return switch (sender) {
            case "servicio", "smtp" -> true;
            case "log" -> false;
            default -> !entorno.getProperty("asistencias.notificaciones.service.url", "").isBlank();
        };
    }
}
