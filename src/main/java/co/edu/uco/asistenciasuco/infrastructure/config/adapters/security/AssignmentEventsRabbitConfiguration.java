package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.RegistrarEventoAsignacionUseCase;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.impl.RegistrarEventoAsignacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.security.RegistroEventoAsignacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq.AssignmentChangedEventMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq.AssignmentEventsRabbitListener;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.audit.RegistroEventoAsignacionAuditAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineRabbitMqProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * SEC-BASELINE-020: Composition Root del consumidor de {@code assignment.changed.&lt;applicationId&gt;} (PDP,
 * Security Baseline 0.1.14). Solo existe con {@code app.adapters.assignment-events.enabled=true}; sin ella el
 * backend arranca igual (la cola queda sin consumidor, el PDP conserva los mensajes).
 *
 * <p>Declara su propia {@link Queue} y {@link Binding}, pero NO el {@link DirectExchange}: el exchange
 * {@code security.assignment.events} es compartido entre aplicaciones consumidoras y lo aprovisiona la
 * plataforma/el PDP (manual del PDP, seccion 6.3); esta aplicacion solo lo referencia por nombre.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.assignment-events", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SecurityBaselineRabbitMqProperties.class)
public class AssignmentEventsRabbitConfiguration {

    @Bean
    public Queue assignmentEventsQueue(final SecurityBaselineRabbitMqProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    public Binding assignmentEventsBinding(final Queue assignmentEventsQueue, final SecurityBaselineRabbitMqProperties properties) {
        return BindingBuilder.bind(assignmentEventsQueue)
                .to(new DirectExchange(properties.exchange(), true, false))
                .with("assignment.changed." + properties.applicationId());
    }

    @Bean
    public RegistroEventoAsignacionPort registroEventoAsignacionPort(final AuditEventJpaRepository repository) {
        return new RegistroEventoAsignacionAuditAdapter(repository);
    }

    @Bean
    public RegistrarEventoAsignacionUseCase registrarEventoAsignacionUseCase(
            final RegistroEventoAsignacionPort registroEventoAsignacionPort,
            final SecurityBaselineRabbitMqProperties properties) {
        return new RegistrarEventoAsignacionUseCaseImpl(registroEventoAsignacionPort, properties.applicationId());
    }

    @Bean
    public AssignmentEventsRabbitListener assignmentEventsRabbitListener(final RegistrarEventoAsignacionUseCase useCase) {
        return new AssignmentEventsRabbitListener(useCase, new AssignmentChangedEventMapper(JsonMapper.builder().build()));
    }
}
