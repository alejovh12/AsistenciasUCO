package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.azure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AzureEventGridWebhookControllerTest {

    private static final String VALIDATION_EVENT_TYPE = "Microsoft.EventGrid.SubscriptionValidationEvent";
    private static final String CHANGE_EVENT_TYPE = "Microsoft.AppConfiguration.KeyValueModified";

    private ProcesarEventoAzureInputPort inputPort;
    private AzureEventGridWebhookController controller;
    private Logger controllerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        inputPort = Mockito.mock(ProcesarEventoAzureInputPort.class);
        controller = new AzureEventGridWebhookController(inputPort);

        controllerLogger = (Logger) LoggerFactory.getLogger(AzureEventGridWebhookController.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        controllerLogger.addAppender(logAppender);
        controllerLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        controllerLogger.detachAppender(logAppender);
    }

    private static Map<String, Object> event(final String eventType, final String subject, final Object data) {
        final Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("subject", subject);
        event.put("data", data);
        return event;
    }

    @Test
    void constructor_rechaza_input_port_nulo() {
        assertThrows(NullPointerException.class, () -> new AzureEventGridWebhookController(null));
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
    void handshake_se_reconoce_por_el_tipo_de_evento_aunque_no_llegue_el_header() {
        final ResponseEntity<?> response = controller.handleAzureEvent(
                null, List.of(event(VALIDATION_EVENT_TYPE, null, Map.of("validationCode", "codigo-por-tipo"))));

        assertEquals(Map.of("validationResponse", "codigo-por-tipo"), response.getBody());
        verifyNoInteractions(inputPort);
    }

    @Test
    void handshake_se_reconoce_por_el_header_aunque_el_tipo_de_evento_sea_otro() {
        final ResponseEntity<?> response = controller.handleAzureEvent(
                "subscriptionvalidation", List.of(event(CHANGE_EVENT_TYPE, null, Map.of("validationCode", "codigo-por-header"))));

        assertEquals(Map.of("validationResponse", "codigo-por-header"), response.getBody());
        verifyNoInteractions(inputPort);
    }

    @Test
    void handshake_sin_validationCode_se_procesa_como_evento_regular() {
        final Map<String, Object> data = Map.of("otro", "valor");

        final ResponseEntity<?> response = controller.handleAzureEvent(
                "SubscriptionValidation", List.of(event(VALIDATION_EVENT_TYPE, "sub", data)));

        assertEquals(200, response.getStatusCode().value());
        verify(inputPort).procesarEvento(VALIDATION_EVENT_TYPE, "sub", data);
    }

    @Test
    void handshake_con_data_que_no_es_un_mapa_se_procesa_como_evento_regular_con_data_vacia() {
        final ResponseEntity<?> response = controller.handleAzureEvent(
                "SubscriptionValidation", List.of(event(VALIDATION_EVENT_TYPE, "sub", "no-es-mapa")));

        assertEquals(200, response.getStatusCode().value());
        verify(inputPort).procesarEvento(VALIDATION_EVENT_TYPE, "sub", Map.of());
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

    @Test
    void procesa_todos_los_eventos_del_lote_y_reporta_el_conteo() {
        final ResponseEntity<?> response = controller.handleAzureEvent(null, List.of(
                event(CHANGE_EVENT_TYPE, "a", Map.of()),
                event(CHANGE_EVENT_TYPE, "b", Map.of())));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new ApiDataResponse<>(true, Map.of("processed", 2)), response.getBody());
        verify(inputPort).procesarEvento(CHANGE_EVENT_TYPE, "a", Map.of());
        verify(inputPort).procesarEvento(CHANGE_EVENT_TYPE, "b", Map.of());
    }

    @Test
    void evento_sin_tipo_ni_subject_se_procesa_con_cadenas_vacias() {
        controller.handleAzureEvent(null, List.of(new HashMap<>()));

        verify(inputPort).procesarEvento("", "", Map.of());
    }

    @Test
    void lote_nulo_o_vacio_responde_cero_procesados_sin_invocar_el_caso_de_uso() {
        final ResponseEntity<?> nulo = controller.handleAzureEvent(null, null);
        final ResponseEntity<?> vacio = controller.handleAzureEvent(null, List.of());

        assertEquals(200, nulo.getStatusCode().value());
        assertEquals(200, vacio.getStatusCode().value());
        verify(inputPort, never()).procesarEvento(anyString(), anyString(), anyMap());
    }

    @Test
    void los_saltos_de_linea_del_evento_no_llegan_al_log_pero_si_al_caso_de_uso() {
        final String tipoMalicioso = CHANGE_EVENT_TYPE + "\r\nFAKE-LOG-LINE level=ERROR";
        final String subjectMalicioso = "asistencias:clave\nFORGED";

        controller.handleAzureEvent(null, List.of(event(tipoMalicioso, subjectMalicioso, Map.of())));

        final ILoggingEvent logged = logAppender.list.stream()
                .filter(e -> e.getFormattedMessage().startsWith("Evento Azure recibido"))
                .findFirst()
                .orElseThrow();
        assertFalse(logged.getFormattedMessage().contains("\n"));
        assertFalse(logged.getFormattedMessage().contains("\r"));
        assertTrue(logged.getFormattedMessage().contains("FORGED"));
        verify(inputPort).procesarEvento(tipoMalicioso, subjectMalicioso, Map.of());
    }
}
