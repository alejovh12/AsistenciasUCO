package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REDIS-001 (TD-084): canal SSE del usuario, sin grupo. Entrega lo que le llega como destinatario y, a titulares y roles con
 * ambito, las solicitudes de sesion de sus grupos; nunca eventos del aula ni de grupos ajenos.
 */
class CanalUsuarioRealtimeTest {

    private static final UUID USUARIO = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTRO = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID GRUPO_PROPIO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO_AJENO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GRUPO_ESTUDIANTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final ReactorRealtimeAdapter realtime = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
    private final ResolverAudienciaRealtimeInputPort resolver = mock(ResolverAudienciaRealtimeInputPort.class);
    private final LocalSseRealtimeStreamGateway gateway = new LocalSseRealtimeStreamGateway(realtime, resolver);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void ambito(final UUID grupo, final AudienciaRealtimeGrupo audiencia) {
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, grupo))).thenReturn(audiencia);
    }

    private static RealtimeEvent grupal(final String tipo, final Object grupo) {
        return RealtimeEvent.of(tipo, Map.of("grupo", grupo.toString()));
    }

    /** Publica los eventos y devuelve los tipos (con su grupo) que llegaron al canal, en orden. */
    private List<String> recibidos(final Flux<RealtimeEventResponse> canal, final RealtimeEvent... eventos) {
        final List<String> llegaron = new CopyOnWriteArrayList<>();
        final CountDownLatch fin = new CountDownLatch(1);
        final Disposable suscripcion = canal.subscribe(r -> {
            if ("fin".equals(r.payload().get("grupo"))) {
                fin.countDown();
            } else {
                llegaron.add(r.type() + ":" + r.payload().get("grupo"));
            }
        });
        try {
            for (final RealtimeEvent evento : eventos) {
                realtime.publish(evento);
            }
            realtime.publish(RealtimeEvent.paraDestinatarios("FALTA_SESION_AVISADA", Map.of("grupo", "fin"), Set.of(USUARIO)));
            assertTrue(fin.await(5, TimeUnit.SECONDS), "el canal no recibio la marca de fin");
            return List.copyOf(llegaron);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } finally {
            suscripcion.dispose();
        }
    }

    @Test
    void entrega_al_destinatario_y_no_a_otros_usuarios() {
        final List<String> llegaron = recibidos(gateway.subscribeUsuario(USUARIO, false),
                RealtimeEvent.paraDestinatarios("FALTA_SESION_AVISADA", Map.of("grupo", "g1"), Set.of(USUARIO)),
                RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", Map.of("grupo", "g2"), Set.of(USUARIO, OTRO)),
                RealtimeEvent.paraDestinatarios("FALTA_SESION_AVISADA", Map.of("grupo", "g3"), Set.of(OTRO)),
                RealtimeEvent.paraDestinatarios("REVISION_CANCELACION_CREADA", Map.of("grupo", "g4"), Set.of(USUARIO)));

        assertEquals(List.of("FALTA_SESION_AVISADA:g1", "ALERTA_INASISTENCIA:g2", "REVISION_CANCELACION_CREADA:g4"), llegaron);
        verify(resolver, never()).execute(any());
    }

    @Test
    void un_tipo_sin_audiencia_destinatario_no_se_entrega_aunque_nombre_al_usuario() {
        final List<String> llegaron = recibidos(gateway.subscribeUsuario(USUARIO, true),
                RealtimeEvent.paraDestinatarios("ASISTENCIA_QR_REGISTRADA", Map.of("grupo", GRUPO_PROPIO.toString()), Set.of(USUARIO)));

        assertTrue(llegaron.isEmpty());
    }

    @Test
    void solicitudes_de_sesion_llegan_al_titular_y_al_ambito_resolviendo_una_vez_por_grupo() {
        ambito(GRUPO_PROPIO, new AudienciaRealtimeGrupo(false, false, true));
        ambito(GRUPO_AJENO, new AudienciaRealtimeGrupo(false, false, false));
        ambito(GRUPO_ESTUDIANTE, new AudienciaRealtimeGrupo(false, true, false));

        final List<String> llegaron = recibidos(gateway.subscribeUsuario(USUARIO, true),
                grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO),
                grupal("SESION_SOLICITUD_CREADA", GRUPO_AJENO),
                grupal("SESION_SOLICITUD_RECHAZADA", GRUPO_PROPIO),
                grupal("SESION_MANUAL_CREADA", GRUPO_ESTUDIANTE),
                grupal("ASISTENCIAS_TOMA_ACTUALIZADAS", GRUPO_PROPIO),
                grupal("SESION_SOLICITUD_CREADA", "no-es-uuid"),
                RealtimeEvent.of("SESION_SOLICITUD_CREADA", Map.of()));

        assertEquals(List.of("SESION_SOLICITUD_CREADA:" + GRUPO_PROPIO, "SESION_SOLICITUD_RECHAZADA:" + GRUPO_PROPIO), llegaron);
        verify(resolver, times(1)).execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO));
        verify(resolver, times(1)).execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_AJENO));
    }

    @Test
    void el_docente_titular_recibe_la_sesion_aprobada() {
        ambito(GRUPO_PROPIO, new AudienciaRealtimeGrupo(true, false, false));

        assertEquals(List.of("SESION_MANUAL_CREADA:" + GRUPO_PROPIO),
                recibidos(gateway.subscribeUsuario(USUARIO, true), grupal("SESION_MANUAL_CREADA", GRUPO_PROPIO)));
    }

    @Test
    void un_estudiante_no_consulta_ambitos() {
        final List<String> llegaron = recibidos(gateway.subscribeUsuario(USUARIO, false), grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO));

        assertTrue(llegaron.isEmpty());
        verify(resolver, never()).execute(any());
    }

    @Test
    void si_la_consulta_de_ambito_falla_el_evento_no_llega_y_se_reintenta_con_el_siguiente() {
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO)))
                .thenThrow(new IllegalStateException("BD caida"))
                .thenReturn(new AudienciaRealtimeGrupo(false, false, true));

        final List<String> llegaron = recibidos(gateway.subscribeUsuario(USUARIO, true),
                grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO),
                grupal("SESION_SOLICITUD_RECHAZADA", GRUPO_PROPIO));

        assertEquals(List.of("SESION_SOLICITUD_RECHAZADA:" + GRUPO_PROPIO), llegaron);
    }

    @Test
    void una_audiencia_nula_no_entrega() {
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO))).thenReturn(null);

        assertTrue(recibidos(gateway.subscribeUsuario(USUARIO, true), grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO)).isEmpty());
    }

    @Test
    void el_usuario_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> gateway.subscribeUsuario(null, true));
    }

    @Test
    void solo_se_omiten_ambitos_si_todos_los_roles_son_estudiante() {
        assertFalse(LocalSseRealtimeStreamGateway.soloEstudiante(null));
        assertFalse(LocalSseRealtimeStreamGateway.soloEstudiante(new TestingAuthenticationToken("u", "p")));
        assertTrue(LocalSseRealtimeStreamGateway.soloEstudiante(new TestingAuthenticationToken("u", "p", "ROLE_ESTUDIANTE")));
        assertFalse(LocalSseRealtimeStreamGateway.soloEstudiante(new TestingAuthenticationToken("u", "p", "ROLE_ESTUDIANTE", "ROLE_DOCENTE")));
        assertFalse(LocalSseRealtimeStreamGateway.soloEstudiante(new TestingAuthenticationToken("u", "p", "ROLE_COORDINADOR")));
    }

    @Test
    void la_version_publica_toma_los_roles_de_la_autenticacion_vigente() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("u", "p",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))));

        StepVerifier.create(gateway.subscribeUsuario(USUARIO))
                .then(() -> realtime.publish(grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO)))
                .then(() -> realtime.publish(RealtimeEvent.paraDestinatarios("FALTA_SESION_AVISADA", Map.of("grupo", "g1"), Set.of(USUARIO))))
                .assertNext(r -> assertEquals("FALTA_SESION_AVISADA", r.type()))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
        verify(resolver, never()).execute(any());
    }

    @Test
    void el_ambito_recordado_vence_y_quien_lo_pierde_deja_de_recibir_sin_reconectar() {
        final java.util.concurrent.atomic.AtomicLong nanos = new java.util.concurrent.atomic.AtomicLong();
        gateway.usarRelojNanos(nanos::get);
        when(resolver.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO)))
                .thenReturn(new AudienciaRealtimeGrupo(false, false, true), new AudienciaRealtimeGrupo(false, false, false));
        final Flux<RealtimeEventResponse> canal = gateway.subscribeUsuario(USUARIO, true);
        final List<String> llegaron = new CopyOnWriteArrayList<>();
        final java.util.concurrent.BlockingQueue<String> marcas = new java.util.concurrent.LinkedBlockingQueue<>();
        final Disposable suscripcion = canal.subscribe(r -> {
            if ("fin".equals(r.payload().get("grupo"))) {
                marcas.add("fin");
            } else {
                llegaron.add(r.type());
            }
        });
        final RealtimeEvent marca = RealtimeEvent.paraDestinatarios("FALTA_SESION_AVISADA", Map.of("grupo", "fin"), Set.of(USUARIO));
        try {
            realtime.publish(grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO));
            realtime.publish(marca);
            assertEquals("fin", marcas.poll(5, TimeUnit.SECONDS));
            realtime.publish(grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO));
            realtime.publish(marca);
            assertEquals("fin", marcas.poll(5, TimeUnit.SECONDS));
            nanos.addAndGet(LocalSseRealtimeStreamGateway.VIGENCIA_AMBITO.toNanos());
            realtime.publish(grupal("SESION_SOLICITUD_CREADA", GRUPO_PROPIO));
            realtime.publish(marca);
            assertEquals("fin", marcas.poll(5, TimeUnit.SECONDS));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } finally {
            suscripcion.dispose();
        }

        assertEquals(List.of("SESION_SOLICITUD_CREADA", "SESION_SOLICITUD_CREADA"), llegaron,
                "dentro de la vigencia usa lo recordado; al vencer se vuelve a resolver y ya no tiene ambito");
        verify(resolver, times(2)).execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO));
    }

    @Test
    void la_memoria_de_ambitos_por_conexion_esta_acotada() {
        final int total = LocalSseRealtimeStreamGateway.MAXIMO_AMBITOS_POR_CONEXION + 1;
        final UUID primero = UUID.randomUUID();
        final Disposable suscripcion = gateway.subscribeUsuario(USUARIO, true).subscribe();
        try {
            for (int i = 0; i < total; i++) {
                realtime.publish(grupal("SESION_SOLICITUD_CREADA", i == 0 ? primero : UUID.randomUUID()));
                verify(resolver, org.mockito.Mockito.timeout(2000).times(i + 1)).execute(any());
            }
            realtime.publish(grupal("SESION_SOLICITUD_CREADA", primero));
            // El primer grupo se olvido al superar el maximo, asi que se vuelve a resolver.
            verify(resolver, org.mockito.Mockito.timeout(2000).times(2))
                    .execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, primero));
        } finally {
            suscripcion.dispose();
        }
    }
}
