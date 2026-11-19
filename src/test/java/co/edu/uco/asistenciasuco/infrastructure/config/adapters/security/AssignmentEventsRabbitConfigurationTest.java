package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.RegistrarEventoAsignacionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.security.RegistroEventoAsignacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq.AssignmentEventsRabbitListener;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineRabbitMqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AssignmentEventsRabbitConfigurationTest {

    private static final UUID APPLICATION_ID = UUID.fromString("a0b42637-39f8-4ddd-bec2-0781ba08e779");
    private static final String EXCHANGE = "security.assignment.events";
    private static final String QUEUE = "security.assignment." + APPLICATION_ID;

    private final AssignmentEventsRabbitConfiguration configuration = new AssignmentEventsRabbitConfiguration();
    private final SecurityBaselineRabbitMqProperties properties =
            new SecurityBaselineRabbitMqProperties(APPLICATION_ID, EXCHANGE, QUEUE);

    @Test
    void configura_la_topologia_y_publica_los_componentes_del_consumidor() {
        final Queue queue = configuration.assignmentEventsQueue(properties);
        assertEquals(QUEUE, queue.getName());
        assertTrue(queue.isDurable());

        final Binding binding = configuration.assignmentEventsBinding(queue, properties);
        assertEquals(QUEUE, binding.getDestination());
        assertEquals(EXCHANGE, binding.getExchange());
        assertEquals("assignment.changed." + APPLICATION_ID, binding.getRoutingKey());

        final RegistroEventoAsignacionPort auditPort =
                configuration.registroEventoAsignacionPort(mock(AuditEventJpaRepository.class));
        assertNotNull(auditPort);

        final RegistrarEventoAsignacionUseCase useCase =
                configuration.registrarEventoAsignacionUseCase(mock(RegistroEventoAsignacionPort.class), properties);
        assertNotNull(useCase);

        final AssignmentEventsRabbitListener listener = configuration.assignmentEventsRabbitListener(useCase);
        assertNotNull(listener);
    }
}
