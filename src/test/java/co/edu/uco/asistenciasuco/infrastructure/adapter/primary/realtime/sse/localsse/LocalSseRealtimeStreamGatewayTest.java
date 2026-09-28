package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeStreamGatewayImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();

    private final ReactorRealtimeAdapter realtimeAdapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final LocalSseRealtimeStreamGateway gateway =
            new LocalSseRealtimeStreamGateway(realtimeAdapter, institutionalScopePort);

    @Test
    void subscribe_autoriza_una_vez_y_mapea_el_evento_del_adapter_a_una_respuesta_http() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);

        StepVerifier.create(gateway.subscribe(USUARIO, GRUPO_PROPIO))
                .then(() -> realtimeAdapter.publish(
                        co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent.of(
                                "ASISTENCIAS_SESION_ACTUALIZADAS",
                                Map.of("grupo", GRUPO_PROPIO.toString(), "sesion", "s1", "totalRegistros", 2)
                        )
                ))
                .assertNext(response -> {
                    assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", response.type());
                    assertEquals(GRUPO_PROPIO.toString(), response.payload().get("grupo"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));

        verify(institutionalScopePort, org.mockito.Mockito.times(1)).canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO);
    }

    @Test
    void subscribe_rechaza_grupo_ajeno_con_forbidden_sin_construir_el_flux() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_AJENO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> gateway.subscribe(USUARIO, GRUPO_AJENO));
    }

    @Test
    void subscribe_filtra_en_memoria_eventos_de_otros_grupos() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);

        StepVerifier.create(gateway.subscribe(USUARIO, GRUPO_PROPIO))
                .then(() -> {
                    realtimeAdapter.publish(co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent.of(
                            "ASISTENCIAS_SESION_ACTUALIZADAS", Map.of("grupo", GRUPO_AJENO.toString())));
                    realtimeAdapter.publish(co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent.of(
                            "ASISTENCIAS_SESION_ACTUALIZADAS", Map.of("grupo", GRUPO_PROPIO.toString())));
                })
                .assertNext(response -> assertEquals(GRUPO_PROPIO.toString(), response.payload().get("grupo")))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void subscribe_no_entrega_eventos_sin_grupo_en_el_payload() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);

        StepVerifier.create(gateway.subscribe(USUARIO, GRUPO_PROPIO))
                .then(() -> {
                    realtimeAdapter.publish(co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent.of(
                            "DIAGNOSTICO", Map.of()));
                    realtimeAdapter.publish(co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent.of(
                            "ASISTENCIAS_SESION_ACTUALIZADAS", Map.of("grupo", GRUPO_PROPIO.toString())));
                })
                .assertNext(response -> assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", response.type()))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void activeSubscribersCount_refleja_el_conteo_del_adapter() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);
        assertEquals(0, gateway.activeSubscribersCount());

        final reactor.core.Disposable subscription = gateway.subscribe(USUARIO, GRUPO_PROPIO).subscribe();
        try {
            assertEquals(1, gateway.activeSubscribersCount());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void emit_construye_el_evento_y_lo_publica_a_traves_del_adapter() {
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);

        StepVerifier.create(gateway.subscribe(USUARIO, GRUPO_PROPIO))
                .then(() -> gateway.emit("GENERAL.CUSTOM_EVENT", Map.of("grupo", GRUPO_PROPIO.toString(), "id", 42)))
                .assertNext(response -> {
                    assertEquals("GENERAL.CUSTOM_EVENT", response.type());
                    assertEquals(42, response.payload().get("id"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
