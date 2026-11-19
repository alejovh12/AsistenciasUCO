package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** SEC-BASELINE-020: lectura campo a campo del JSON de AssignmentChangedEventV1 (manual del PDP, seccion 5). */
class AssignmentChangedEventMapperTest {

    private final AssignmentChangedEventMapper mapper = new AssignmentChangedEventMapper(JsonMapper.builder().build());

    private static final String JSON_EJEMPLO_MANUAL = """
            {
              "eventId": "11111111-1111-1111-1111-111111111111",
              "schemaVersion": 1,
              "eventType": "ROLE_ASSIGNED",
              "occurredOn": "2026-11-17T20:00:00Z",
              "tenantId": "universidad-uco",
              "applicationId": "41ce1b83-a94e-49d7-a1fb-ee6d1769ca0d",
              "applicationName": "demo-integracion-v2",
              "userId": "user-demo",
              "correlationId": "manual-rabbit-001",
              "assignmentId": "assignment-demo-001",
              "roleId": "role-demo-001",
              "roleName": "DEMO_ROLE",
              "profileAssignmentId": null,
              "profileId": null,
              "profileName": null,
              "roles": [{"assignmentId":"assignment-demo-001","roleId":"role-demo-001","roleName":"DEMO_ROLE"}]
            }
            """;

    @Test
    void json_de_ejemplo_del_manual_se_mapea_1_a_1_ignorando_roles() {
        final EventoAsignacionSeguridad evento = mapper.leer(JSON_EJEMPLO_MANUAL.getBytes(StandardCharsets.UTF_8));

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), evento.eventId());
        assertEquals(1, evento.schemaVersion());
        assertEquals("ROLE_ASSIGNED", evento.eventType());
        assertEquals(OffsetDateTime.parse("2026-11-17T20:00:00Z"), evento.occurredOn());
        assertEquals("universidad-uco", evento.tenantId());
        assertEquals(UUID.fromString("41ce1b83-a94e-49d7-a1fb-ee6d1769ca0d"), evento.applicationId());
        assertEquals("demo-integracion-v2", evento.applicationName());
        assertEquals("user-demo", evento.userId());
        assertEquals("manual-rabbit-001", evento.correlationId());
        assertEquals("assignment-demo-001", evento.assignmentId());
        assertEquals("role-demo-001", evento.roleId());
        assertEquals("DEMO_ROLE", evento.roleName());
        assertNull(evento.profileAssignmentId());
        assertNull(evento.profileId());
        assertNull(evento.profileName());
    }

    @Test
    void campos_adicionales_desconocidos_no_rompen_la_lectura() {
        final String json = "{\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"schemaVersion\":1,"
                + "\"campoFuturoDesconocido\":{\"x\":1}}";
        final EventoAsignacionSeguridad evento = mapper.leer(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), evento.eventId());
    }

    @Test
    void payload_no_json_se_rechaza() {
        assertThrows(RuntimeException.class, () -> mapper.leer("no es json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void payload_json_que_no_es_objeto_se_rechaza() {
        assertThrows(IllegalArgumentException.class, () -> mapper.leer("[1,2]".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void payload_vacio_se_rechaza() {
        assertThrows(IllegalArgumentException.class, () -> mapper.leer(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> mapper.leer(null));
    }
}
