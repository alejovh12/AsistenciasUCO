package co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.ProcesarEventoAzureUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;

import java.util.Map;
import java.util.Objects;

public class ProcesarEventoAzureUseCaseImpl implements ProcesarEventoAzureUseCase {
    private static final String CLAVE_EVENT_TYPE = "eventType";
    private static final String CLAVE_OBJECT_NAME = "ObjectName";

    private final CatalogInvalidationPort catalogInvalidationPort;
    private final RealtimePublisherPort realtimePublisherPort;

    public ProcesarEventoAzureUseCaseImpl(
            final CatalogInvalidationPort catalogInvalidationPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        this.catalogInvalidationPort = Objects.requireNonNull(catalogInvalidationPort, "CatalogInvalidationPort es obligatorio.");
        this.realtimePublisherPort = Objects.requireNonNull(realtimePublisherPort, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public void procesarEvento(final String eventType, final String subject, final Map<String, Object> data) {
        if (eventType == null || eventType.isBlank()) {
            return;
        }

        if (eventType.startsWith("Microsoft.AppConfiguration.")) {
            procesarEventoAppConfiguration(eventType, subject, data);
        } else if (eventType.startsWith("Microsoft.KeyVault.")) {
            procesarEventoKeyVault(subject, data);
        }
    }

    private void procesarEventoAppConfiguration(final String eventType, final String subject, final Map<String, Object> data) {
        final String rawKey;
        if (data != null && data.containsKey("key") && data.get("key") != null) {
            rawKey = data.get("key").toString().trim();
        } else if (subject != null && !subject.isBlank()) {
            rawKey = subject.trim();
        } else {
            catalogInvalidationPort.invalidateAll();
            return;
        }

        if (rawKey.startsWith("messages:user:")) {
            final String code = rawKey.substring("messages:user:".length());
            catalogInvalidationPort.invalidateMessage(code);
            realtimePublisherPort.publish(RealtimeEvent.of("MESSAGE_UPDATED", Map.of(
                    "code", code,
                    "type", "USER",
                    CLAVE_EVENT_TYPE, eventType
            )));
        } else if (rawKey.startsWith("messages:technical:")) {
            final String code = rawKey.substring("messages:technical:".length());
            catalogInvalidationPort.invalidateMessage(code);
        } else if (rawKey.contains(":")) {
            final int colonIdx = rawKey.indexOf(":");
            final String group = rawKey.substring(0, colonIdx);
            final String key = rawKey.substring(colonIdx + 1);
            catalogInvalidationPort.invalidateParameter(group, key);
            realtimePublisherPort.publish(RealtimeEvent.of("PARAMETER_UPDATED", Map.of(
                    "group", group,
                    "key", key,
                    CLAVE_EVENT_TYPE, eventType
            )));
        } else {
            catalogInvalidationPort.invalidateParameter("", rawKey);
            realtimePublisherPort.publish(RealtimeEvent.of("PARAMETER_UPDATED", Map.of(
                    "key", rawKey,
                    CLAVE_EVENT_TYPE, eventType
            )));
        }
    }

    private void procesarEventoKeyVault(final String subject, final Map<String, Object> data) {
        final String secretName;
        if (data != null && data.containsKey(CLAVE_OBJECT_NAME) && data.get(CLAVE_OBJECT_NAME) != null) {
            secretName = data.get(CLAVE_OBJECT_NAME).toString().trim();
        } else if (subject != null && !subject.isBlank()) {
            secretName = subject.trim();
        } else {
            return;
        }

        catalogInvalidationPort.invalidateSecret(secretName);
    }
}