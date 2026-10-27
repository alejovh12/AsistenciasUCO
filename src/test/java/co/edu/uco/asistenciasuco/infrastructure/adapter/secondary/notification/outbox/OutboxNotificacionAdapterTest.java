package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxNotificacionAdapterTest {

    private NotificacionOutboxRepository repository;
    private NotificacionOutboxDispatcher dispatcher;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private OutboxNotificacionAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacionOutboxRepository.class);
        dispatcher = mock(NotificacionOutboxDispatcher.class);
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        adapter = new OutboxNotificacionAdapter(repository, dispatcher, meterRegistry, objectMapper);
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @DisplayName("BE-O01: notificar(null) lanza NullPointerException")
    void BE_O01_notificar_nulo_lanza_npe() {
        assertThrows(NullPointerException.class, () -> adapter.notificar(null));
    }

    @Test
    @DisplayName("BE-O02: Notificacion valida llama al repositorio y luego signal() al despachador")
    void BE_O02_notificacion_valida_guarda_y_senala() {
        final UUID corrId = UUID.randomUUID();
        CorrelationIdContext.set(corrId);

        final UUID estudianteId = UUID.randomUUID();
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                "Asunto prueba",
                "Mensaje prueba",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", estudianteId, "Pedro", "pedro@uco.edu.co")),
                Map.of("grupoId", "123")
        );

        adapter.notificar(notificacion);

        final ArgumentCaptor<RegistroNotificacion> captor = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(repository).registrar(captor.capture());
        final RegistroNotificacion registro = captor.getValue();

        assertEquals(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, registro.tipo());
        assertEquals("Asunto prueba", registro.asunto());
        assertEquals("Mensaje prueba", registro.mensaje());
        assertEquals(corrId, registro.idCorrelacion());
        assertTrue(registro.destinatariosJson().contains(estudianteId.toString()));
        assertTrue(registro.datosJson().contains("123"));

        verify(dispatcher).signal();

        final double accepted = meterRegistry.get("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "accepted")
                .counter().count();
        assertEquals(1.0, accepted);
    }

    @Test
    @DisplayName("BE-O03: Destinatarios no serializan nombre ni correo")
    void BE_O03_destinatarios_sin_nombre_ni_correo() {
        final UUID usuarioId = UUID.randomUUID();
        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("DOCENTE", usuarioId, "Carlos Sanchez", "carlos@uco.edu.co")),
                Map.of()
        );

        adapter.notificar(notificacion);

        final ArgumentCaptor<RegistroNotificacion> captor = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(repository).registrar(captor.capture());
        final String destJson = captor.getValue().destinatariosJson();

        assertTrue(destJson.contains("DOCENTE"));
        assertTrue(destJson.contains(usuarioId.toString()));
        assertFalse(destJson.contains("Carlos Sanchez"), "El JSON no debe contener el nombre");
        assertFalse(destJson.contains("carlos@uco.edu.co"), "El JSON no debe contener el correo");
    }

    @Test
    @DisplayName("BE-O04: Sin destinatarios no llama al repositorio e incrementa skipped")
    void BE_O04_sin_destinatarios_omite_guardado() {
        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(),
                Map.of()
        );

        adapter.notificar(notificacion);

        verify(repository, never()).registrar(any());
        verify(dispatcher, never()).signal();

        final double skipped = meterRegistry.get("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "skipped")
                .counter().count();
        assertEquals(1.0, skipped);
    }

    @Test
    @DisplayName("BE-O05: Excepcion en repositorio no se propaga e incrementa failed")
    void BE_O05_excepcion_en_repositorio_capturada_e_incrementa_failed() {
        doThrow(new RuntimeException("DB Connection Timeout")).when(repository).registrar(any());

        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Nombre", "correo@uco.edu.co")),
                Map.of()
        );

        assertDoesNotThrow(() -> adapter.notificar(notificacion));

        final double failed = meterRegistry.get("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "failed")
                .counter().count();
        assertEquals(1.0, failed);
    }

    @Test
    @DisplayName("BE-O06: Sin contexto de correlacion tolera idCorrelacion nulo")
    void BE_O06_sin_correlacion_tolera_nulo() {
        CorrelationIdContext.clear();

        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Nombre", "correo@uco.edu.co")),
                Map.of()
        );

        adapter.notificar(notificacion);

        final ArgumentCaptor<RegistroNotificacion> captor = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(repository).registrar(captor.capture());
        assertNull(captor.getValue().idCorrelacion());
    }

    @Test
    @DisplayName("BE-O07: Datos con mas de 8000 caracteres se rechaza internamente como fallo")
    void BE_O07_datos_exceden_limite_falla_sin_propagar() {
        final String valorLargo = String.join("", Collections.nCopies(8001, "X"));
        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Nombre", "correo@uco.edu.co")),
                Map.of("clave", valorLargo)
        );

        assertDoesNotThrow(() -> adapter.notificar(notificacion));
        verify(repository, never()).registrar(any());

        final double failed = meterRegistry.get("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "failed")
                .counter().count();
        assertEquals(1.0, failed);
    }

    @Test
    @DisplayName("BE-O08: Fallo de serializacion JSON incrementa failed sin propagar")
    void BE_O08_fallo_serializacion_json_incrementa_failed() throws Exception {
        final com.fasterxml.jackson.databind.ObjectMapper mockMapper = mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        doThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Error JSON"))
                .when(mockMapper).writeValueAsString(any());

        final OutboxNotificacionAdapter customAdapter = new OutboxNotificacionAdapter(
                repository, dispatcher, meterRegistry, mockMapper
        );

        final Notificacion notificacion = new Notificacion(
                "TIPO_TEST",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Nombre", "correo@uco.edu.co")),
                Map.of("clave", "valor")
        );

        assertDoesNotThrow(() -> customAdapter.notificar(notificacion));
        verify(repository, never()).registrar(any());

        final double failed = meterRegistry.get("notification.publish.total")
                .tag("provider", "outbox")
                .tag("outcome", "failed")
                .counter().count();
        assertEquals(1.0, failed);
    }

    @Test
    @DisplayName("BE-O09: Con span activo registra traceId y spanId")
    void BE_O09_con_span_activo_registra_trazabilidad() {
        final io.opentelemetry.api.trace.SpanContext spanContext = io.opentelemetry.api.trace.SpanContext.create(
                "4bf92f3577b34da6a3ce929d0e0e4736",
                "00f067aa0ba902b7",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );
        final io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.wrap(spanContext);

        try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
            final Notificacion notificacion = new Notificacion(
                    "TIPO_TEST",
                    "Asunto",
                    "Mensaje",
                    List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Nombre", "correo@uco.edu.co")),
                    Map.of()
            );

            adapter.notificar(notificacion);

            final ArgumentCaptor<RegistroNotificacion> captor = ArgumentCaptor.forClass(RegistroNotificacion.class);
            verify(repository).registrar(captor.capture());
            assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", captor.getValue().traceId());
            assertEquals("00f067aa0ba902b7", captor.getValue().spanId());
        }
    }
}
