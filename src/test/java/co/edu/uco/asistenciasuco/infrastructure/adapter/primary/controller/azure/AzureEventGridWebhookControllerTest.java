package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.azure;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class AzureEventGridWebhookControllerTest {

    private ProcesarEventoAzureInputPort inputPort;
    private AzureEventGridWebhookController controller;

    @BeforeEach
    void setUp() {
        inputPort = Mockito.mock(ProcesarEventoAzureInputPort.class);
        controller = new AzureEventGridWebhookController(inputPort);
    }

    @Test
    void responde_handshake_de_suscripcion_con_validationResponse() {
        final Map<String, Object> validationEvent = Map.of(
                "eventType", "Microsoft.EventGrid.SubscriptionValidationEvent",
                "data", Map.of("validationCode", "echo-token-123")
        );

        final ResponseEntity<?> response = controller.handleAzureEvent("SubscriptionValidation", List.of(validationEvent));
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("validationResponse", "echo-token-123"), response.getBody());
    }

    @Test
    void procesa_evento_regular_e_invoca_input_port() {
        final Map<String, Object> changeEvent = Map.of(
                "eventType", "Microsoft.AppConfiguration.KeyValueModified",
                "subject", "asistencias:max_inasistencias",
                "data", Map.of("key", "asistencias:max_inasistencias")
        );

        final ResponseEntity<?> response = controller.handleAzureEvent(null, List.of(changeEvent));
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(inputPort).procesarEvento(
                eq("Microsoft.AppConfiguration.KeyValueModified"),
                eq("asistencias:max_inasistencias"),
                eq(Map.of("key", "asistencias:max_inasistencias"))
        );
    }
}