package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.8, 3.11 SSE): la politica compara el tipo por igualdad exacta; en B toda audiencia es el
 * docente titular y el gateway sigue entregando al titular los eventos de su grupo, incluido {@code SESION_CANCELADA}.
 */
class RealtimeAudienciaPolicyTest {

    private final RealtimeAudienciaPolicy policy = RealtimeAudienciaPolicy.porDefecto();

    @Test
    void los_tipos_de_negocio_se_entregan_al_titular_por_igualdad_exacta() {
        // CULM-001 C.1 (contrato 3.6.3): SESION_CANCELADA se abre al estudiante del grupo y al ambito de lectura, ademas del titular.
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR, RealtimeAudienciaPolicy.Audiencia.ESTUDIANTE_GRUPO,
                RealtimeAudienciaPolicy.Audiencia.AMBITO_GRUPO), policy.audienciasDe("SESION_CANCELADA"));
        assertTrue(policy.entregaA("ASISTENCIAS_SESION_ACTUALIZADAS", RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR));
        // Un tipo de /realtime/emit (topic.action) o un prefijo nunca coincide con un tipo de negocio: va solo al titular.
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe("sesion_cancelada.emit"));
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe("SESION_CANCELADA_X"));
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe(null));
    }

    @Test
    void un_tipo_sin_audiencias_declaradas_queda_solo_para_el_titular() {
        final RealtimeAudienciaPolicy vacia = new RealtimeAudienciaPolicy(Map.of("X", Set.of()));
        assertTrue(vacia.entregaA("X", RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR));
        assertThrows(NullPointerException.class, () -> new RealtimeAudienciaPolicy(null));
    }

    @Test
    void el_gateway_entrega_sesion_cancelada_al_titular_del_grupo_y_filtra_los_tipos_sin_audiencia() {
        final UUID docente = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final ResolverAudienciaRealtimeInputPort scope = mock(ResolverAudienciaRealtimeInputPort.class);
        when(scope.execute(new ResolverAudienciaRealtimeInputPort.Comando(docente, grupo))).thenReturn(new AudienciaRealtimeGrupo(true, false, false));
        final ReactorRealtimeAdapter adapter = mock(ReactorRealtimeAdapter.class);
        when(adapter.events()).thenReturn(Flux.just(
                RealtimeEvent.of("SESION_CANCELADA", Map.of("grupo", grupo.toString(), "sesion", "s1", "tipo", "DOCENTE")),
                RealtimeEvent.of("SECRETO", Map.of("grupo", grupo.toString())),
                RealtimeEvent.of("SESION_CANCELADA", Map.of("grupo", UUID.randomUUID().toString()))));
        final RealtimeAudienciaPolicy soloCancelada = new RealtimeAudienciaPolicy(Map.of(
                "SESION_CANCELADA", Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR)));

        final Flux<RealtimeEventResponse> eventos =
                new LocalSseRealtimeStreamGateway(adapter, scope, soloCancelada).subscribe(docente, grupo);

        StepVerifier.create(eventos)
                .assertNext(evento -> assertEquals("SESION_CANCELADA", evento.type()))
                .assertNext(evento -> assertEquals("SECRETO", evento.type()))
                .expectComplete()
                .verify(Duration.ofSeconds(5));
        assertFalse(soloCancelada.audienciasDe("SECRETO").isEmpty());
        assertThrows(NullPointerException.class, () -> new LocalSseRealtimeStreamGateway(adapter, scope, null));
    }

    @Test
    void B2_los_cinco_tipos_de_tomas_y_qr_van_solo_al_titular_por_igualdad_exacta() {
        for (final String tipo : java.util.List.of("TOMA_ABIERTA", "TOMA_CERRADA", "ASISTENCIA_QR_ROTADO",
                "ASISTENCIA_QR_REGISTRADA")) {
            assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe(tipo));
        }
        // CULM-001 C.1 (contrato 3.6.3): el conteo sin identidad de la toma llega tambien al estudiante y al ambito de lectura.
        assertTrue(policy.entregaA("ASISTENCIAS_TOMA_ACTUALIZADAS", RealtimeAudienciaPolicy.Audiencia.ESTUDIANTE_GRUPO));
        // Un tipo de /realtime/emit parecido no coincide con el del QR (que lleva el token).
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe("asistencia_qr.rotado"));
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR), policy.audienciasDe("ASISTENCIA_QR_ROTADO_X"));
    }

    // CULM-001 D.9-c / D.3 (CONTRACT §4.5): PARAMETER_UPDATED es de configuracion y solo del ADMINISTRADOR; nunca llega por el
    // canal de grupo, cuyos suscriptores no tienen esa audiencia.
    @Test
    void parameter_updated_es_un_tipo_del_catalogo_solo_para_la_audiencia_administrador() {
        assertEquals(Set.of(RealtimeAudienciaPolicy.Audiencia.ADMINISTRADOR), policy.audienciasDe("PARAMETER_UPDATED"));
        assertTrue(policy.tiposConocidos().contains("PARAMETER_UPDATED"));
        for (final RealtimeAudienciaPolicy.Audiencia deGrupo : new RealtimeAudienciaPolicy.Audiencia[]{
                RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR, RealtimeAudienciaPolicy.Audiencia.ESTUDIANTE_GRUPO,
                RealtimeAudienciaPolicy.Audiencia.AMBITO_GRUPO}) {
            assertFalse(policy.entrega("PARAMETER_UPDATED", Set.of(deGrupo), UUID.randomUUID(), Set.of()));
        }
    }

    @Test
    void tiposConocidos_lista_los_tipos_de_la_tabla_y_es_inmutable() {
        assertTrue(policy.tiposConocidos().containsAll(Set.of("SESION_CANCELADA", "ALERTA_INASISTENCIA", "TOMA_ABIERTA", "ASISTENCIA_QR_REGISTRADA")));
        assertFalse(policy.tiposConocidos().contains("NO_EXISTE"));
        assertThrows(UnsupportedOperationException.class, () -> policy.tiposConocidos().add("X"));
    }
}
