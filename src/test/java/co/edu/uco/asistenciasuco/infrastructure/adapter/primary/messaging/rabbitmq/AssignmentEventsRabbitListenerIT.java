package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SEC-BASELINE-020: contra el RabbitMQ real del lab Security Baseline 0.1.14 y la BD real. Requiere el lab arriba
 * (docker compose -f infra/security-baseline/compose.yaml up -d) con la cola/binding de la seccion 4 del manual del
 * PDP ya creados para ASISTENCIAS_PDP_APPLICATION_ID, y las variables RABBITMQ_USERNAME/RABBITMQ_PASSWORD del
 * lab.env local exportadas en el entorno que ejecuta {@code mvnw -Pintegration verify}. Limpia sus filas en
 * {@code @AfterEach}.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.assignment-events.enabled=true",
        "app.providers.security-baseline-rabbitmq.application-id=a0b42637-39f8-4ddd-bec2-0781ba08e779",
        "app.providers.security-baseline-rabbitmq.exchange=security.assignment.events",
        "app.providers.security-baseline-rabbitmq.queue=security.assignment.a0b42637-39f8-4ddd-bec2-0781ba08e779",
        "spring.rabbitmq.host=${RABBITMQ_HOST:localhost}",
        "spring.rabbitmq.port=${RABBITMQ_AMQP_PORT:25672}",
        "spring.rabbitmq.virtual-host=/security",
        "spring.rabbitmq.username=${RABBITMQ_USERNAME}",
        "spring.rabbitmq.password=${RABBITMQ_PASSWORD}"
})
@MockitoBean(types = JwtDecoder.class)
class AssignmentEventsRabbitListenerIT {

    private static final String EXCHANGE = "security.assignment.events";
    private static final String APPLICATION_ID = "a0b42637-39f8-4ddd-bec2-0781ba08e779";
    private static final String ROUTING_KEY = "assignment.changed." + APPLICATION_ID;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> creados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (final UUID id : creados) {
            jdbcTemplate.update("DELETE FROM dbo.AuditoriaEvento WHERE id = ?", id.toString());
        }
        creados.clear();
    }

    private String evento(final UUID eventId, final String applicationId, final int schemaVersion) {
        return """
                {"eventId":"%s","schemaVersion":%d,"eventType":"ROLE_ASSIGNED","occurredOn":"%s",
                "tenantId":"universidad-uco","applicationId":"%s","applicationName":"asistencias-uco",
                "userId":"user-it","correlationId":"%s","assignmentId":"assignment-it-001",
                "roleId":"role-it-001","roleName":"IT_ROLE"}
                """.formatted(eventId, schemaVersion, Instant.now(), applicationId, UUID.randomUUID());
    }

    private void publicar(final String json) {
        rabbitTemplate.send(EXCHANGE, ROUTING_KEY,
                new org.springframework.amqp.core.Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    private long contarPorId(final UUID id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.AuditoriaEvento WHERE id = ?", Long.class, id.toString());
    }

    private void esperarHasta(final Callable<Boolean> condicion) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(condicion);
    }

    @Test
    void mensaje_valido_nuevo_queda_auditado_exactamente_una_vez() {
        final UUID eventId = UUID.randomUUID();
        creados.add(eventId);

        publicar(evento(eventId, APPLICATION_ID, 1));

        esperarHasta(() -> contarPorId(eventId) == 1L);
        assertEquals(1L, contarPorId(eventId));
    }

    @Test
    void mensaje_repetido_no_duplica_la_auditoria() throws InterruptedException {
        final UUID eventId = UUID.randomUUID();
        creados.add(eventId);
        final String json = evento(eventId, APPLICATION_ID, 1);

        publicar(json);
        esperarHasta(() -> contarPorId(eventId) == 1L);
        publicar(json);
        Thread.sleep(1500); // da tiempo a que el listener confirme (ack) la segunda entrega como duplicado

        assertEquals(1L, contarPorId(eventId));
    }

    @Test
    void application_id_ajeno_no_se_audita() throws InterruptedException {
        final UUID eventId = UUID.randomUUID();
        creados.add(eventId);

        publicar(evento(eventId, UUID.randomUUID().toString(), 1));
        Thread.sleep(1500); // da tiempo a que el listener rechace el mensaje antes de comprobar que no se audito

        assertEquals(0L, contarPorId(eventId));
    }

    @Test
    void schema_version_no_soportada_no_se_audita() throws InterruptedException {
        final UUID eventId = UUID.randomUUID();
        creados.add(eventId);

        publicar(evento(eventId, APPLICATION_ID, 2));
        Thread.sleep(1500); // da tiempo a que el listener rechace el mensaje antes de comprobar que no se audito

        assertEquals(0L, contarPorId(eventId));
    }
}
