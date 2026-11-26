package co.edu.uco.asistenciasuco.infrastructure.cache.decorador;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.ConsultarResumenMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.local.CaffeineCacheConsultas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * REDIS-001 (TD-083): cuando el docente cambia asistencias, cierra una toma, cancela una sesion o se matricula alguien en
 * el grupo, el portal de los estudiantes y del docente de ese grupo se recarga desde la BD en la siguiente consulta.
 */
class InvalidacionCachePorEventosTest {

    private static final UUID ESTUDIANTE = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DOCENTE = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTRO_GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final CacheConsultas cache = new CaffeineCacheConsultas(ttl(), 100, new MetricasCache(new SimpleMeterRegistry(), "local"));
    private final ConsultasCacheadasPostProcessor processor = new ConsultasCacheadasPostProcessor(cache, null, null);
    private final InvalidacionCachePorEventos invalidacion = new InvalidacionCachePorEventos(() -> cache);

    private final AtomicInteger cargasMaterias = new AtomicInteger();
    private final AtomicInteger cargasResumen = new AtomicInteger();
    private final AtomicInteger cargasDocente = new AtomicInteger();

    private final ConsultarMateriasEstudianteInputPort materias = envolver((ConsultarMateriasEstudianteInputPort) actor -> {
        cargasMaterias.incrementAndGet();
        return List.of(new MateriaEstudianteDTO(UUID.randomUUID(), "Calculo", GRUPO, "G1"));
    });
    private final ConsultarResumenMateriasEstudianteInputPort resumen = envolver((ConsultarResumenMateriasEstudianteInputPort) actor -> {
        cargasResumen.incrementAndGet();
        return new ResumenMateriasEstudianteDTO(1, 0, BigDecimal.ZERO, 3);
    });
    private final ConsultarAsignaturasDocenteInputPort asignaturas = envolver((ConsultarAsignaturasDocenteInputPort) actor -> {
        cargasDocente.incrementAndGet();
        return List.of(new AsignaturaDocenteDTO(UUID.randomUUID(), "Calculo", GRUPO, "G1", null, null, null, null, 30));
    });

    private static Map<RegionCache, Duration> ttl() {
        final Map<RegionCache, Duration> mapa = new EnumMap<>(RegionCache.class);
        for (final RegionCache region : RegionCache.values()) {
            mapa.put(region, Duration.ofMinutes(1));
        }
        return mapa;
    }

    @SuppressWarnings("unchecked")
    private <T> T envolver(final T bean) {
        return (T) processor.postProcessAfterInitialization(bean, "bean");
    }

    private void consultarTodo() {
        materias.execute(ESTUDIANTE);
        resumen.execute(ESTUDIANTE);
        asignaturas.execute(DOCENTE);
    }

    private static RealtimeEvent evento(final String tipo, final Object grupo) {
        final Map<String, Object> payload = new HashMap<>();
        if (grupo != null) {
            payload.put("grupo", grupo.toString());
        }
        return RealtimeEvent.of(tipo, payload);
    }

    @Test
    void un_cambio_de_asistencias_del_grupo_recarga_el_portal_del_estudiante_y_del_docente() {
        consultarTodo();
        consultarTodo();
        assertEquals(1, cargasMaterias.get());
        assertEquals(1, cargasResumen.get());
        assertEquals(1, cargasDocente.get());

        invalidacion.accept(evento("ASISTENCIAS_TOMA_ACTUALIZADAS", GRUPO));
        consultarTodo();

        assertEquals(2, cargasMaterias.get());
        assertEquals(2, cargasResumen.get(), "el resumen queda etiquetado junto con las materias");
        assertEquals(2, cargasDocente.get());
    }

    @Test
    void cada_evento_de_grupo_invalida() {
        for (final String tipo : InvalidacionCachePorEventos.TIPOS_GRUPO) {
            materias.execute(ESTUDIANTE);
            final int antes = cargasMaterias.get();
            invalidacion.accept(evento(tipo, GRUPO));
            materias.execute(ESTUDIANTE);
            assertEquals(antes + 1, cargasMaterias.get(), tipo);
        }
    }

    @Test
    void eventos_de_otro_grupo_o_de_otro_tipo_no_invalidan() {
        consultarTodo();
        invalidacion.accept(evento("ASISTENCIAS_TOMA_ACTUALIZADAS", OTRO_GRUPO));
        invalidacion.accept(evento("ASISTENCIA_QR_REGISTRADA", GRUPO));
        invalidacion.accept(evento("ASISTENCIA_QR_ROTADO", GRUPO));
        invalidacion.accept(evento("SESION_CANCELADA", "no-es-uuid"));
        invalidacion.accept(evento("SESION_CANCELADA", null));
        invalidacion.accept(null);
        consultarTodo();

        assertEquals(1, cargasMaterias.get());
        assertEquals(1, cargasResumen.get());
        assertEquals(1, cargasDocente.get());
    }

    @Test
    void una_alerta_de_inasistencia_recarga_el_portal_del_estudiante_destinatario() {
        consultarTodo();
        invalidacion.accept(RealtimeEvent.paraDestinatarios("ALERTA_INASISTENCIA", Map.of(), Set.of(ESTUDIANTE)));
        consultarTodo();

        assertEquals(2, cargasMaterias.get());
        assertEquals(2, cargasResumen.get());
        assertEquals(1, cargasDocente.get());
    }

    @Test
    void matricularse_por_qr_recarga_el_portal_del_estudiante() {
        consultarTodo();
        final MatricularEstudianteAutenticadoInputPort matricula = envolver(
                (MatricularEstudianteAutenticadoInputPort) comando -> new MatricularEstudianteAutenticadoInputPort.Resultado(OTRO_GRUPO));

        assertEquals(OTRO_GRUPO, matricula.execute(new MatricularEstudianteAutenticadoInputPort.Comando("t", null, "ip", ESTUDIANTE)).idGrupo());
        consultarTodo();

        assertEquals(2, cargasMaterias.get());
        assertEquals(2, cargasResumen.get());
        assertEquals(1, cargasDocente.get());
    }

    @Test
    void matricula_sin_usuario_no_toca_la_cache() {
        final CacheConsultas espia = mock(CacheConsultas.class);
        final ConsultasCacheadasPostProcessor conEspia = new ConsultasCacheadasPostProcessor(espia, null, null);
        final MatricularEstudianteAutenticadoInputPort matricula = (MatricularEstudianteAutenticadoInputPort) conEspia
                .postProcessAfterInitialization((MatricularEstudianteAutenticadoInputPort) comando -> null, "bean");

        matricula.execute(null);
        matricula.execute(new MatricularEstudianteAutenticadoInputPort.Comando("t", null, "ip", null));

        verifyNoInteractions(espia);
    }

    @Test
    void el_adaptador_sse_avisa_al_observador_en_cada_entrega_y_aisla_sus_fallos() {
        final ReactorRealtimeAdapter adaptador = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
        adaptador.alEntregar(evento -> {
            throw new IllegalStateException("observador roto");
        });
        adaptador.alEntregar(invalidacion);
        consultarTodo();

        adaptador.publish(evento("TOMA_CERRADA", GRUPO));
        materias.execute(ESTUDIANTE);
        adaptador.entregarRelevado(evento("SESION_CANCELADA", GRUPO));
        materias.execute(ESTUDIANTE);

        assertEquals(3, cargasMaterias.get());
    }
}
