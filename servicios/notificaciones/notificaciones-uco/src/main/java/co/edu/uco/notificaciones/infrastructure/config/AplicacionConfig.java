package co.edu.uco.notificaciones.infrastructure.config;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.application.port.in.PlantillasUseCase;
import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.application.port.out.DespertadorPort;
import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.application.service.Despachador;
import co.edu.uco.notificaciones.application.service.EnviadorCorreo;
import co.edu.uco.notificaciones.application.service.GestionNotificaciones;
import co.edu.uco.notificaciones.application.service.GestionPlantillas;
import co.edu.uco.notificaciones.application.service.RenderizadorPlantillas;
import co.edu.uco.notificaciones.application.service.TopeEnvios;
import co.edu.uco.notificaciones.infrastructure.adapter.out.smtp.JakartaMailCorreoTransport;
import co.edu.uco.notificaciones.infrastructure.adapter.out.smtp.LogCorreoTransport;
import co.edu.uco.notificaciones.infrastructure.scheduler.DespachadorScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/** Composition Root: la capa de aplicacion no usa estereotipos Spring; se cablea aqui. */
@Configuration(proxyBeanMethods = false)
public class AplicacionConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    ConfiguracionCanal configuracionCanal(final NotificacionesProperties propiedades) {
        return ConfiguracionFactory.canal(propiedades);
    }

    @Bean
    ConfiguracionDespachador configuracionDespachador(final NotificacionesProperties propiedades) {
        return ConfiguracionFactory.despachador(propiedades.dispatcher());
    }

    @Bean
    CorreoTransportPort correoTransport(final ConfiguracionCanal canal, final NotificacionesProperties propiedades) {
        if (ConfiguracionCanal.CANAL_SMTP.equals(canal.canal())) {
            return new JakartaMailCorreoTransport(canal, propiedades.smtp());
        }
        return new LogCorreoTransport();
    }

    @Bean
    RenderizadorPlantillas renderizadorPlantillas(final PlantillaRepositoryPort plantillas, final MetricasPort metricas,
                                                  final ConfiguracionCanal canal) {
        return new RenderizadorPlantillas(plantillas, metricas, canal.platformUrl(), cargarLayout());
    }

    @Bean
    TopeEnvios topeEnvios(final ConfiguracionCanal canal, final Clock reloj, final MetricasPort metricas) {
        return new TopeEnvios(canal.ratePerMinute(), canal.ratePerDay(), reloj, metricas);
    }

    @Bean
    EnviadorCorreo enviadorCorreo(final PlantillaRepositoryPort plantillas, final RenderizadorPlantillas renderizador,
                                  final CorreoTransportPort transporte, final TopeEnvios tope, final ConfiguracionCanal canal,
                                  final MetricasPort metricas) {
        return new EnviadorCorreo(plantillas, renderizador, transporte, tope, canal, metricas);
    }

    @Bean
    Despachador despachador(final NotificacionRepositoryPort repositorio, final EnviadorCorreo enviador,
                            final ConfiguracionDespachador config, final MetricasPort metricas, final Clock reloj) {
        return new Despachador(repositorio, enviador, config, metricas, reloj);
    }

    @Bean
    DespachadorScheduler despachadorScheduler(final Despachador despachador) {
        return new DespachadorScheduler(despachador);
    }

    @Bean
    NotificacionesUseCase notificacionesUseCase(final NotificacionRepositoryPort repositorio, final PlantillaRepositoryPort plantillas,
                                                final DespertadorPort despertador, final MetricasPort metricas,
                                                final EnviadorCorreo enviador, final ConfiguracionCanal canal,
                                                final ConfiguracionDespachador despachador, final Clock reloj) {
        return new GestionNotificaciones(repositorio, plantillas, despertador, metricas, enviador, canal, despachador, reloj);
    }

    @Bean
    PlantillasUseCase plantillasUseCase(final PlantillaRepositoryPort plantillas, final RenderizadorPlantillas renderizador, final Clock reloj) {
        return new GestionPlantillas(plantillas, renderizador, reloj);
    }

    private static String cargarLayout() {
        try {
            return new String(new ClassPathResource("plantillas/layout.html").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException("No se pudo cargar plantillas/layout.html", ex);
        }
    }
}
