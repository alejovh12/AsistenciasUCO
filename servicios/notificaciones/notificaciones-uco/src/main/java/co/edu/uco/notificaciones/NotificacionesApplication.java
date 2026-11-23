package co.edu.uco.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import co.edu.uco.notificaciones.infrastructure.config.NotificacionesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NotificacionesProperties.class)
public class NotificacionesApplication {

    public static void main(final String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
