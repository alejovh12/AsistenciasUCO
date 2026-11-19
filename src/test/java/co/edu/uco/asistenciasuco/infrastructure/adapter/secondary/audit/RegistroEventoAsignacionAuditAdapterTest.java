package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.audit;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SEC-BASELINE-020: mapea el evento a dbo.AuditoriaEvento con eventId como PK (idempotencia). */
class RegistroEventoAsignacionAuditAdapterTest {

    private final AuditEventJpaRepository repository = mock(AuditEventJpaRepository.class);
    private final RegistroEventoAsignacionAuditAdapter adapter = new RegistroEventoAsignacionAuditAdapter(repository);

    @Test
    void mapea_el_evento_y_usa_eventId_como_id_de_la_fila() {
        final UUID eventId = UUID.randomUUID();
        final UUID applicationId = UUID.fromString("a0b42637-39f8-4ddd-bec2-0781ba08e779");
        final EventoAsignacionSeguridad evento = new EventoAsignacionSeguridad(eventId, 1, "ROLE_ASSIGNED",
                OffsetDateTime.parse("2026-11-17T20:00:00Z"), "universidad-uco", applicationId, "asistencias-uco",
                "user-demo", UUID.randomUUID().toString(), "assignment-demo-001", "role-demo-001", "DEMO_ROLE",
                null, null, null);
        when(repository.insertIfAbsent(any())).thenReturn(true);

        final boolean registrado = adapter.registrarSiNuevo(evento);

        assertTrue(registrado);
        final ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).insertIfAbsent(captor.capture());
        final AuditEvent auditado = captor.getValue();
        assertEquals(eventId, auditado.id());
        assertEquals("user-demo", auditado.actorId());
        assertEquals(AuditActorType.SYSTEM, auditado.actorType());
        assertEquals("ROLE_ASSIGNED", auditado.action());
        assertEquals("SECURITY_ASSIGNMENT_EVENT", auditado.resourceType());
        assertEquals("assignment-demo-001", auditado.resourceId());
        assertEquals(AuditOutcome.SUCCESS, auditado.outcome());
        assertEquals(applicationId.toString(), auditado.metadata().get("applicationId"));
        assertEquals("role-demo-001", auditado.metadata().get("roleId"));
        // httpMethod/path/httpStatus son NOT NULL en dbo.AuditoriaEvento (tabla pensada para auditoria HTTP).
        assertEquals("AMQP", auditado.httpMethod());
        assertEquals("rabbitmq:security.assignment.events", auditado.path());
        assertEquals(200, auditado.httpStatus());
    }

    @Test
    void duplicado_devuelve_false() {
        final EventoAsignacionSeguridad evento = new EventoAsignacionSeguridad(UUID.randomUUID(), 1, "ROLE_ASSIGNED",
                OffsetDateTime.now(), "universidad-uco", UUID.randomUUID(), "asistencias-uco", "user-demo",
                null, "assignment-demo-001", "role-demo-001", "DEMO_ROLE", null, null, null);
        when(repository.insertIfAbsent(any())).thenReturn(false);

        assertFalse(adapter.registrarSiNuevo(evento));
    }

    @Test
    void sin_assignmentId_usa_profileAssignmentId_como_resourceId() {
        final EventoAsignacionSeguridad evento = new EventoAsignacionSeguridad(UUID.randomUUID(), 1, "PROFILE_ASSIGNED",
                OffsetDateTime.now(), "universidad-uco", UUID.randomUUID(), "asistencias-uco", "user-demo",
                null, null, null, null, "profile-assignment-001", "profile-001", "DOCENCIA");
        when(repository.insertIfAbsent(any())).thenReturn(true);

        adapter.registrarSiNuevo(evento);

        final ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).insertIfAbsent(captor.capture());
        assertEquals("profile-assignment-001", captor.getValue().resourceId());
    }
}
