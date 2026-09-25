package co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.ProcesarEventoAzureUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;

import java.util.Map;
import java.util.Objects;

public class ProcesarEventoAzureUseCaseImpl implements ProcesarEventoAzureUseCase {

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
            procesarEventoKeyVault(eventType, subject, data);
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
                    "eventType", eventType
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
                    "eventType", eventType
            )));
        } else {
            catalogInvalidationPort.invalidateParameter("", rawKey);
            realtimePublisherPort.publish(RealtimeEvent.of("PARAMETER_UPDATED", Map.of(
                    "key", rawKey,
                    "eventType", eventType
            )));
        }
    }

    private void procesarEventoKeyVault(final String eventType, final String subject, final Map<String, Object> data) {
        final String secretName;
        if (data != null && data.containsKey("ObjectName") && data.get("ObjectName") != null) {
            secretName = data.get("ObjectName").toString().trim();
        } else if (subject != null && !subject.isBlank()) {
            secretName = subject.trim();
        } else {
            return;
        }

        catalogInvalidationPort.invalidateSecret(secretName);
    }
}