package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.azure;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controlador primario para recibir eventos webhook de Azure Event Grid.
 *
 * <p>Soporta tanto el handshake de validación de suscripción de Event Grid (SubscriptionValidationEvent)
 * como los eventos de cambio y borrado de Azure App Configuration y Azure Key Vault.</p>
 */
@RestController
@RequestMapping("/api/v1/internal/azure-events")
public class AzureEventGridWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEventGridWebhookController.class);

    private static final String SUBSCRIPTION_VALIDATION_EVENT_TYPE = "Microsoft.EventGrid.SubscriptionValidationEvent";
    private static final String SUBSCRIPTION_VALIDATION_HEADER = "SubscriptionValidation";

    private final ProcesarEventoAzureInputPort procesarEventoAzureInputPort;

    public AzureEventGridWebhookController(final ProcesarEventoAzureInputPort procesarEventoAzureInputPort) {
        this.procesarEventoAzureInputPort = Objects.requireNonNull(
                procesarEventoAzureInputPort,
                "ProcesarEventoAzureInputPort es obligatorio."
        );
    }

    @PostMapping
    public ResponseEntity<?> handleAzureEvent(
            @RequestHeader(value = "aeg-event-type", required = false) final String aegEventType,
            @RequestBody final List<Map<String, Object>> events
    ) {
        if (events == null || events.isEmpty()) {
            return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of("processed", 0)));
        }

        // 1. Manejo de handshake de validación de Event Grid
        for (final Map<String, Object> event : events) {
            final String eventType = Objects.toString(event.get("eventType"), "");
            if (SUBSCRIPTION_VALIDATION_EVENT_TYPE.equalsIgnoreCase(eventType) || SUBSCRIPTION_VALIDATION_HEADER.equalsIgnoreCase(aegEventType)) {
                final Object dataObj = event.get("data");
                if (dataObj instanceof Map<?, ?> dataMap && dataMap.containsKey("validationCode")) {
                    final String validationCode = Objects.toString(dataMap.get("validationCode"), "");
                    LOGGER.info("Respondiendo al handshake de validación de Azure Event Grid con validationCode recibido.");
                    return ResponseEntity.ok(Map.of("validationResponse", validationCode));
                }
            }
        }

        // 2. Procesamiento de eventos regulares (App Configuration / Key Vault)
        int processedCount = 0;
        for (final Map<String, Object> event : events) {
            final String eventType = Objects.toString(event.get("eventType"), "");
            final String subject = Objects.toString(event.get("subject"), "");
            final Object dataObj = event.get("data");

            @SuppressWarnings("unchecked")
            final Map<String, Object> data = (dataObj instanceof Map) ? (Map<String, Object>) dataObj : Map.of();

            LOGGER.info("Evento Azure recibido: eventType={}, subject={}", eventType, subject);
            procesarEventoAzureInputPort.procesarEvento(eventType, subject, data);
            processedCount++;
        }

        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of("processed", processedCount)));
    }
}
