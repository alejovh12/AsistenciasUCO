package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (contrato 3.6, 3.9.1): tabla de audiencias por tipo y canal de grupo abierto a los cinco roles con filtro de ambito. */
class RealtimeAudienciaPolicyAmbitoTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID OTRO_ESTUDIANTE = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID SIN_AMBITO = UUID.randomUUID();

    private final RealtimeAudienciaPolicy politica = RealtimeAudienciaPolicy.porDefecto();

    @Test
    void la_tabla_del_contrato_se_cumple_por_tipo_exacto() {
        for (final String tipo : new String[]{"ASISTENCIAS_SESION_ACTUALIZADAS", "ASISTENCIAS_TOMA_ACTUALIZADAS", "SESION_CANCELADA"}) {
            assertEquals(EnumSet.of(Audiencia.DOCENTE_TITULAR, Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO), politica.audienciasDe(tipo), tipo);
        }
        for (final String tipo : new String[]{"ALERTA_INASISTENCIA", "REVISION_CANCELACION_CREADA", "REVISION_CANCELACION_DECIDIDA",
                "REVISION_CANCELACION_RESUELTA", "REVISION_CANCELACION_REASIGNADA"}) {
            assertEquals(EnumSet.of(Audiencia.DOCENTE_TITULAR, Audiencia.DESTINATARIO), politica.audienciasDe(tipo), tipo);
        }
        for (final String tipo : new String[]{"ASISTENCIA_REGISTRADA", "MATRICULA_QR_ROTADO", "ESTUDIANTE_MATRICULADO", "TOMA_ABIERTA", "TOMA_CERRADA",
                "ASISTENCIA_QR_ROTADO", "ASISTENCIA_QR_REGISTRADA", "no.listado", "ASISTENCIAS_SESION_ACTUALIZADAS.x"}) {
            assertEquals(EnumSet.of(Audiencia.DOCENTE_TITULAR), politica.audienciasDe(tipo), tipo);
        }
        assertEquals(EnumSet.of(Audiencia.DOCENTE_TITULAR), politica.audienciasDe(null));
        assertTrue(politica.entregaA("SESION_CANCELADA", Audiencia.ESTUDIANTE_GRUPO));
    }

    @Test
    void entrega_exige_interseccion_o_pertenencia_a_los_destinatarios() {
        final Set<Audiencia> estudiante = EnumSet.of(Audiencia.ESTUDIANTE_GRUPO);
        assertTrue(politica.entrega("SESION_CANCELADA", estudiante, ESTUDIANTE, Set.of()));
        assertFalse(politica.entrega("ASISTENCIA_QR_ROTADO", estudiante, ESTUDIANTE, Set.of(ESTUDIANTE)));
        assertFalse(politica.entrega("ALERTA_INASISTENCIA", estudiante, ESTUDIANTE, Set.of()));
        assertFalse(politica.entrega("ALERTA_INASISTENCIA", estudiante, OTRO_ESTUDIANTE, Set.of(ESTUDIANTE)));
        assertTrue(politica.entrega("ALERTA_INASISTENCIA", estudiante, ESTUDIANTE, Set.of(ESTUDIANTE)));
        assertTrue(politica.entrega("ALERTA_INASISTENCIA", EnumSet.of(Audiencia.DOCENTE_TITULAR), DOCENTE, Set.of()));
        assertFalse(politica.entrega("ALERTA_INASISTENCIA", EnumSet.of(Audiencia.AMBITO_GRUPO), COORDINADOR, Set.of()));
        assertFalse(politica.entrega("ALERTA_INASISTENCIA", estudiante, null, Set.of(ESTUDIANTE)));
        assertFalse(politica.entrega("ALERTA_INASISTENCIA", estudiante, ESTUDIANTE, null));
        assertFalse(politica.entrega("TOMA_CERRADA", EnumSet.of(Audiencia.DESTINATARIO), ESTUDIANTE, Set.of(ESTUDIANTE)));
    }

    private LocalSseRealtimeStreamGateway gateway(final ReactorRealtimeAdapter adapter) {
        final ResolverAudienciaRealtimeInputPort resolver = mock(ResolverAudienciaRealtimeInputPort.class);
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(DOCENTE, GRUPO))).thenReturn(new AudienciaRealtimeGrupo(true, false, false));
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(ESTUDIANTE, GRUPO))).thenReturn(new AudienciaRealtimeGrupo(false, true, false));
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(COORDINADOR, GRUPO))).thenReturn(new AudienciaRealtimeGrupo(false, false, true));
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(SIN_AMBITO, GRUPO))).thenReturn(new AudienciaRealtimeGrupo(false, false, false));
        return new LocalSseRealtimeStreamGateway(adapter, resolver);
    }

    @Test
    void sin_ninguna_audiencia_el_gateway_responde_403() {
        final ReactorRealtimeAdapter adapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        assertThrows(ForbiddenException.class, () -> gateway(adapter).subscribe(SIN_AMBITO, GRUPO));
        assertThrows(NullPointerException.class, () -> gateway(adapter).subscribe(null, GRUPO));
        assertThrows(NullPointerException.class, () -> gateway(adapter).subscribe(DOCENTE, null));
    }

    @Test
    void el_estudiante_recibe_conteos_y_cancelaciones_pero_no_la_alerta_de_otro_ni_el_token_del_qr() {
        final ReactorRealtimeAdapter adapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        final LocalSseRealtimeStreamGateway gateway = gateway(adapter);
        final Map<String, Object> payload = Map.of("grupo", GRUPO.toString());
        StepVerifier.create(gateway.subscribe(ESTUDIANTE, GRUPO))
                .then(() -> {
                    adapter.publish(RealtimeEvent.of("ASISTENCIA_QR_ROTADO", payload));
                    adapter.publish(RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", payload, Set.of(OTRO_ESTUDIANTE)));
                    adapter.publish(RealtimeEvent.of("ASISTENCIAS_SESION_ACTUALIZADAS", payload));
                    adapter.publish(RealtimeEvent.of("SESION_CANCELADA", payload));
                    adapter.publish(RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", payload, Set.of(ESTUDIANTE)));
                    adapter.publish(RealtimeEvent.of("SESION_CANCELADA", Map.of("grupo", UUID.randomUUID().toString())));
                })
                .assertNext(r -> assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", r.type()))
                .assertNext(r -> assertEquals("SESION_CANCELADA", r.type()))
                .assertNext(r -> {
                    assertEquals("ALERTA_INASISTENCIA", r.type());
                    assertFalse(r.toString().contains("destinatariosUsuario"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void el_coordinador_con_ambito_no_recibe_alertas_y_el_docente_si_las_recibe_como_titular() {
        final ReactorRealtimeAdapter adapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        final LocalSseRealtimeStreamGateway gateway = gateway(adapter);
        final Map<String, Object> payload = Map.of("grupo", GRUPO.toString());
        StepVerifier.create(gateway.subscribe(COORDINADOR, GRUPO))
                .then(() -> {
                    adapter.publish(RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", payload, Set.of(ESTUDIANTE)));
                    adapter.publish(RealtimeEvent.paraDestinatarios("REVISION_CANCELACION_RESUELTA", payload, Set.of(COORDINADOR)));
                    adapter.publish(RealtimeEvent.of("TOMA_CERRADA", payload));
                    adapter.publish(RealtimeEvent.of("ASISTENCIAS_TOMA_ACTUALIZADAS", payload));
                })
                .assertNext(r -> assertEquals("REVISION_CANCELACION_RESUELTA", r.type()))
                .assertNext(r -> assertEquals("ASISTENCIAS_TOMA_ACTUALIZADAS", r.type()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
        StepVerifier.create(gateway.subscribe(DOCENTE, GRUPO))
                .then(() -> adapter.publish(RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", payload, Set.of(ESTUDIANTE))))
                .assertNext(r -> assertEquals("ALERTA_INASISTENCIA", r.type()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
        assertEquals(0, gateway.activeSubscribersCount());
        gateway.emit("topic.action", payload);
    }

    @Test
    void realtime_event_conserva_el_constructor_anterior_y_el_contexto_de_observabilidad() {
        final RealtimeEvent evento = new RealtimeEvent(UUID.randomUUID(), "X", java.time.Instant.now(), null, null, null, Map.of());
        assertTrue(evento.destinatariosUsuario().isEmpty());
        final RealtimeEvent dirigido = RealtimeEvent.paraDestinatarios("X", Map.of(), Set.of(ESTUDIANTE)).withObservabilityContext("c", "t", "s");
        assertEquals(Set.of(ESTUDIANTE), dirigido.destinatariosUsuario());
        assertEquals("c", dirigido.correlationId());
        assertTrue(new RealtimeEvent(UUID.randomUUID(), "X", java.time.Instant.now(), null, null, null, null, null).destinatariosUsuario().isEmpty());
    }
}
