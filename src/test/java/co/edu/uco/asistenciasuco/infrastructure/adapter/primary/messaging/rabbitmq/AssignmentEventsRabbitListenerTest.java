package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.rabbitmq;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort.EventoAsignacionInvalidoException;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort.ResultadoRegistro;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** SEC-BASELINE-020: decide ack/reject/nack segun el resultado del caso de uso (manual del PDP, seccion 6.4). */
class AssignmentEventsRabbitListenerTest {

    private static final long DELIVERY_TAG = 7L;
    private static final UUID APPLICATION_ID = UUID.fromString("a0b42637-39f8-4ddd-bec2-0781ba08e779");

    private final RegistrarEventoAsignacionInputPort useCase = mock(RegistrarEventoAsignacionInputPort.class);
    private final AssignmentChangedEventMapper mapper = mock(AssignmentChangedEventMapper.class);
    private final Channel channel = mock(Channel.class);
    private final AssignmentEventsRabbitListener listener = new AssignmentEventsRabbitListener(useCase, mapper);

    private EventoAsignacionSeguridad eventoValido() {
        return new EventoAsignacionSeguridad(UUID.randomUUID(), 1, "ROLE_ASSIGNED", OffsetDateTime.now(),
                "universidad-uco", APPLICATION_ID, "asistencias-uco", "user-demo", UUID.randomUUID().toString(),
                "assignment-demo-001", "role-demo-001", "DEMO_ROLE", null, null, null);
    }

    @Test
    void payload_ilegible_se_rechaza_sin_requeue_y_no_llama_al_caso_de_uso() throws IOException {
        when(mapper.leer(any())).thenThrow(new IllegalArgumentException("no es json"));

        listener.procesar("no es json".getBytes(StandardCharsets.UTF_8), channel, DELIVERY_TAG);

        verify(channel).basicReject(DELIVERY_TAG, false);
        verifyNoMoreInteractions(useCase);
    }

    @Test
    void contrato_invalido_se_rechaza_sin_requeue() throws IOException {
        final EventoAsignacionSeguridad evento = eventoValido();
        when(mapper.leer(any())).thenReturn(evento);
        when(useCase.procesar(evento)).thenThrow(new EventoAsignacionInvalidoException("applicationId ajeno"));

        listener.procesar(new byte[]{1}, channel, DELIVERY_TAG);

        verify(channel).basicReject(DELIVERY_TAG, false);
    }

    @Test
    void procesado_confirma_con_ack() throws IOException {
        final EventoAsignacionSeguridad evento = eventoValido();
        when(mapper.leer(any())).thenReturn(evento);
        when(useCase.procesar(evento)).thenReturn(ResultadoRegistro.PROCESADO);

        listener.procesar(new byte[]{1}, channel, DELIVERY_TAG);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void duplicado_tambien_confirma_con_ack() throws IOException {
        final EventoAsignacionSeguridad evento = eventoValido();
        when(mapper.leer(any())).thenReturn(evento);
        when(useCase.procesar(evento)).thenReturn(ResultadoRegistro.DUPLICADO);

        listener.procesar(new byte[]{1}, channel, DELIVERY_TAG);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void falla_generica_reintenta_con_nack_y_requeue() throws IOException {
        final EventoAsignacionSeguridad evento = eventoValido();
        when(mapper.leer(any())).thenReturn(evento);
        when(useCase.procesar(evento)).thenThrow(new RuntimeException("BD caida"));

        listener.procesar(new byte[]{1}, channel, DELIVERY_TAG);

        verify(channel).basicNack(eq(DELIVERY_TAG), eq(false), eq(true));
    }

    @Test
    void correlation_id_ajeno_no_valido_no_rompe_el_procesamiento() throws IOException {
        final EventoAsignacionSeguridad evento = new EventoAsignacionSeguridad(UUID.randomUUID(), 1, "ROLE_ASSIGNED",
                OffsetDateTime.now(), "universidad-uco", APPLICATION_ID, "asistencias-uco", "user-demo",
                "no-es-un-uuid", "assignment-demo-001", "role-demo-001", "DEMO_ROLE", null, null, null);
        when(mapper.leer(any())).thenReturn(evento);
        when(useCase.procesar(evento)).thenReturn(ResultadoRegistro.PROCESADO);

        assertDoesNotThrow(() -> listener.procesar(new byte[]{1}, channel, DELIVERY_TAG));

        verify(channel).basicAck(DELIVERY_TAG, false);
    }
}
