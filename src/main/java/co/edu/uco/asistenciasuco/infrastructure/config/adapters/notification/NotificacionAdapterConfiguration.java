package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root de notificaciones. Mientras el componente institucional (correo, WhatsApp, SMS) no este lanzado,
 * el provider por defecto solo registra el aviso en el log; al integrarlo, su configuracion aporta otro
 * {@link NotificacionPort} y este deja de crearse.
 */
@Configuration(proxyBeanMethods = false)
public class NotificacionAdapterConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificacionPort.class)
    public NotificacionPort notificacionPort() {
        return new LogNotificacionAdapter();
    }
}
