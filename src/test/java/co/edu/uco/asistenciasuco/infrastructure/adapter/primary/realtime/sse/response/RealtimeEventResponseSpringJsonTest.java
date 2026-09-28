package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response;

import co.edu.uco.asistenciasuco.infrastructure.config.jackson.JacksonInputConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TD-042 (LB-001B.4A): el contrato realtime exige {@code occurredAt} como ISO-8601 UTC con
 * milisegundos y sufijo Z, serializado por el {@link JsonMapper} REAL de Spring Boot
 * ({@link JacksonAutoConfiguration} + el customizer de produccion {@link JacksonInputConfig},
 * mas application.yml de test), no por {@code JsonMapper.builder().build()}.
 * Sin base de datos, Redis ni servidor: el contexto solo contiene la configuracion Jackson.
 */
@SpringBootTest(
        classes = {JacksonAutoConfiguration.class, JacksonInputConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RealtimeEventResponseSpringJsonTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-23T18:52:28.123Z");
    private static final String EXPECTED = "\"occurredAt\":\"2026-09-23T18:52:28.123Z\"";

    @Autowired
    private JsonMapper springJsonMapper;

    @Test
    void mapper_real_de_spring_serializa_occurredAt_iso_utc_con_milisegundos() {
        final String json = springJsonMapper.writeValueAsString(response());

        assertTrue(json.contains(EXPECTED), json);
    }

    @Test
    void conversor_http_de_spring_mvc_con_el_mapper_real_serializa_occurredAt_iso_utc() throws Exception {
        final MockHttpOutputMessage output = new MockHttpOutputMessage();

        new JacksonJsonHttpMessageConverter(springJsonMapper)
                .write(response(), MediaType.APPLICATION_JSON, output);

        final String json = output.getBodyAsString();
        assertTrue(json.contains(EXPECTED), json);
    }

    private static RealtimeEventResponse response() {
        return new RealtimeEventResponse(UUID.randomUUID(), "ASISTENCIAS_SESION_ACTUALIZADAS",
                OCCURRED_AT, "correlation-id", Map.of("grupo", UUID.randomUUID().toString()));
    }
}
