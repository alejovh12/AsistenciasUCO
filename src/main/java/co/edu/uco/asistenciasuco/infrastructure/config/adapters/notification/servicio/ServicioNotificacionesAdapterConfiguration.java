package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.AdministracionNotificacionServicioAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.NotificacionesServiceClient;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service.ServicioNotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.MarcadorDinamicoProvider;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.TokenVerificacionCorreoJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.TokenVerificacionCorreoRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.VerificacionCorreoMarcadorProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxAdapterCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.condition.OutboxSenderServicioCondition;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.security.SecureRandom;

/**
 * Composition Root del Servicio de Notificaciones UCO: cliente HTTP con circuit breaker, canal de salida del outbox
 * ({@code servicio}), administracion de plantillas y cola, y salud. Sin URL configurada el cliente existe pero responde «no
 * disponible», el canal de salida es el de log y la administracion responde 503.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificacionesServiceProperties.class)
public class ServicioNotificacionesAdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificacionesAdapterConfiguration.class);
    static final String CIRCUIT_BREAKER = "notificacionesServicio";

    @Bean
    @ConditionalOnMissingBean(ContactoDestinatarioResolver.class)
    public ContactoDestinatarioResolver contactoDestinatarioResolver(final EntityManager entityManager) {
        return new ContactoDestinatarioJpaRepository(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(NotificacionesServiceClient.class)
    public NotificacionesServiceClient notificacionesServiceClient(
            final NotificacionesServiceProperties properties,
            final ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistry
    ) {
        final CircuitBreaker breaker = circuitBreakerRegistry.getIfAvailable(CircuitBreakerRegistry::ofDefaults)
                .circuitBreaker(CIRCUIT_BREAKER);
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        return new NotificacionesServiceClient(httpClient, properties, breaker, new ObjectMapper());
    }

    @Bean
    @ConditionalOnMissingBean(AdministracionNotificacionPort.class)
    public AdministracionNotificacionPort administracionNotificacionPort(
            final NotificacionesServiceClient cliente,
            final NotificacionesServiceProperties properties,
            final ContactoDestinatarioResolver contactoResolver
    ) {
        return new AdministracionNotificacionServicioAdapter(cliente, properties, contactoResolver, new ObjectMapper());
    }

    @Bean
    @ConditionalOnMissingBean(TokenVerificacionCorreoRepository.class)
    @Conditional({OutboxAdapterCondition.class, OutboxSenderServicioCondition.class})
    public TokenVerificacionCorreoRepository tokenVerificacionCorreoRepository(final EntityManager entityManager) {
        return new TokenVerificacionCorreoJpaRepository(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(MarcadorDinamicoProvider.class)
    @Conditional({OutboxAdapterCondition.class, OutboxSenderServicioCondition.class})
    public MarcadorDinamicoProvider marcadorDinamicoProvider(
            final TokenVerificacionCorreoRepository tokenRepository,
            final NotificacionesServiceProperties properties
    ) {
        return new VerificacionCorreoMarcadorProvider(tokenRepository, properties.platformUrl(), new SecureRandom());
    }

    /**
     * Canal de salida del outbox hacia el servicio. Falla al arrancar si se pidio {@code sender=servicio} sin URL: una
     * configuracion asi dejaria todos los avisos reintentandose sin destino.
     */
    @Bean
    @Conditional({OutboxAdapterCondition.class, OutboxSenderServicioCondition.class})
    public NotificacionChannelSender servicioNotificacionChannelSender(
            final NotificacionesServiceClient cliente,
            final NotificacionesServiceProperties properties,
            final ContactoDestinatarioResolver contactoResolver,
            final MarcadorDinamicoProvider marcadorDinamicoProvider,
            final MeterRegistry meterRegistry
    ) {
        if (!properties.configurado()) {
            throw new IllegalStateException("El canal de notificaciones es el servicio pero NOTIFICACIONES_SERVICE_URL "
                    + "(asistencias.notificaciones.service.url) esta vacia.");
        }
        if (properties.apiKey().isEmpty()) {
            LOGGER.warn("NOTIFICACIONES_API_KEY esta vacia: el servicio de notificaciones respondera 401 a cada aviso.");
        }
        return new ServicioNotificacionChannelSender(cliente, contactoResolver, marcadorDinamicoProvider, properties, meterRegistry);
    }

    /** Componente {@code notificacionesService} de /actuator/health; solo si hay URL configurada. */
    @Bean
    @ConditionalOnExpression("!'${asistencias.notificaciones.service.url:}'.isBlank()")
    public NotificacionesServiceHealthIndicator notificacionesServiceHealthIndicator(final NotificacionesServiceClient cliente) {
        return new NotificacionesServiceHealthIndicator(cliente);
    }
}
