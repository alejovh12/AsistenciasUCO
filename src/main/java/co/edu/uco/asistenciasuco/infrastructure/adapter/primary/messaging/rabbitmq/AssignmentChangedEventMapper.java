package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * SEC-BASELINE-020: lee el JSON de {@code AssignmentChangedEventV1} (manual del PDP, seccion 5) con lectura
 * campo a campo (no deserializacion por record): una propiedad adicional desconocida no rompe la lectura, igual
 * que {@code MessageUcoLabEventListener}. Un campo ausente o invalido se traduce en un valor vacio/invalido que el
 * caso de uso rechaza como contrato invalido; nunca lanza aqui una excepcion distinta a un payload no legible.
 */
public class AssignmentChangedEventMapper {

    private final ObjectMapper objectMapper;

    public AssignmentChangedEventMapper(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper es obligatorio.");
    }

    public EventoAsignacionSeguridad leer(final byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Evento de asignacion vacio.");
        }
        final JsonNode nodo = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        if (!nodo.isObject()) {
            throw new IllegalArgumentException("El evento de asignacion no es un objeto JSON.");
        }
        return new EventoAsignacionSeguridad(
                uuid(nodo, "eventId"),
                nodo.path("schemaVersion").asInt(-1),
                texto(nodo, "eventType"),
                fecha(nodo, "occurredOn"),
                texto(nodo, "tenantId"),
                uuid(nodo, "applicationId"),
                texto(nodo, "applicationName"),
                texto(nodo, "userId"),
                texto(nodo, "correlationId"),
                texto(nodo, "assignmentId"),
                texto(nodo, "roleId"),
                texto(nodo, "roleName"),
                texto(nodo, "profileAssignmentId"),
                texto(nodo, "profileId"),
                texto(nodo, "profileName"));
    }

    private String texto(final JsonNode nodo, final String campo) {
        return nodo.path(campo).asString(null);
    }

    private UUID uuid(final JsonNode nodo, final String campo) {
        final String valor = texto(nodo, campo);
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private OffsetDateTime fecha(final JsonNode nodo, final String campo) {
        final String valor = texto(nodo, campo);
        if (valor == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (final RuntimeException e) {
            return null;
        }
    }
}
