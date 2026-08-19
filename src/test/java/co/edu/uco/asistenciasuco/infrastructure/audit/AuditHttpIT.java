package co.edu.uco.asistenciasuco.infrastructure.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=mock"
)
class AuditHttpIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuditEventJdbcRepository auditEventJdbcRepository;

    private final RestTemplate restTemplate = buildRestTemplate();

    @Test
    void operacion_exitosa_persiste_auditoria_success_recuperable_desde_db() {
        final String correlationId = UUID.randomUUID().toString();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);

        final ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/asistencias/revisiones",
                HttpMethod.POST,
                new HttpEntity<>("""
                        {
                          "asistencia":"00000000-0000-0000-0000-000000000101",
                          "motivo":"Validar asistencia."
                        }
                        """, headers),
                String.class
        );

        assertEquals(202, response.getStatusCode().value());
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-Id"));

        final AuditEvent event = auditEventJdbcRepository.findLatestByCorrelationId(correlationId).orElseThrow();
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals("SOLICITAR_REVISION_ASISTENCIA", event.action());
        assertEquals("ASISTENCIA", event.resourceType());
        assertEquals("00000000-0000-0000-0000-000000000101", event.resourceId());
        assertEquals(correlationId, event.correlationId());
        assertEquals(AuditActorType.ANONYMOUS, event.actorType());
        assertTrue(event.errorCode() == null || event.errorCode().isBlank());
        assertFalse(event.traceId() == null || event.traceId().isBlank());
        assertEquals("HTTP", event.metadata().get("handlerType"));
    }

    @Test
    void operacion_fallida_persiste_auditoria_failure_con_errorCode() {
        final String correlationId = UUID.randomUUID().toString();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);

        final String body = """
                {
                  "tipoIdentificacionId":"13641bab-e3cd-485c-b275-47e7b731e18c",
                  "numeroIdentificacion":789456123,
                  "primerApellido":"PEREZ",
                  "segundoApellido":"GOMEZ",
                  "primerNombre":"ANA",
                  "segundoNombre":"MARIA",
                  "correo":"audit.failure.%s@uco.edu.co",
                  "password":"Clave123!"
                }
                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        final UUID grupoId = UUID.randomUUID();
        final ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/grupos/" + grupoId + "/estudiantes",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-Id"));
        assertTrue(response.getBody().contains("\"code\":\"ERR_GRUPO_NO_HABILITADO\""));

        final AuditEvent event = auditEventJdbcRepository.findLatestByCorrelationId(correlationId).orElseThrow();
        assertEquals(AuditOutcome.FAILURE, event.outcome());
        assertEquals("REGISTRAR_ESTUDIANTE_EN_GRUPO", event.action());
        assertEquals("GRUPO", event.resourceType());
        assertEquals(grupoId.toString(), event.resourceId());
        assertEquals("ERR_GRUPO_NO_HABILITADO", event.errorCode());
        assertEquals(409, event.httpStatus());
        assertFalse(event.traceId() == null || event.traceId().isBlank());
        assertFalse(event.spanId() == null || event.spanId().isBlank());
        assertEquals("HTTP", event.metadata().get("handlerType"));
    }

    private RestTemplate buildRestTemplate() {
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }
}
