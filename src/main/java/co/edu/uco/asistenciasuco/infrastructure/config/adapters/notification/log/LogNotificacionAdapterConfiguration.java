package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.log;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.LogAdapterCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root del adaptador de log para notificaciones.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationAdapterProperties.class)
public class LogNotificacionAdapterConfiguration {

    @Bean
    @Conditional(LogAdapterCondition.class)
    public LogNotificacionAdapter logNotificacionAdapter() {
        return new LogNotificacionAdapter();
    }
}
