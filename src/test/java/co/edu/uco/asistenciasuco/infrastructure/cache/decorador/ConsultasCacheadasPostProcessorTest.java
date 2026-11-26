package co.edu.uco.asistenciasuco.infrastructure.cache.decorador;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.ConsultarPlanEstudioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.ConsultarResumenMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.MetricasCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import co.edu.uco.asistenciasuco.infrastructure.cache.local.CaffeineCacheConsultas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REDIS-001: cada InputPort cacheado responde lo mismo que el original, consulta la fuente una sola vez por clave,
 * nunca comparte el resultado de un usuario con otro y el registro por QR invalida lo del estudiante.
 */
class ConsultasCacheadasPostProcessorTest {

    private static final UUID ACTOR_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private final CacheConsultas cache = new CaffeineCacheConsultas(ttl(), 100, new MetricasCache(new SimpleMeterRegistry(), "local"));
    private final co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort ambito =
            mock(co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort.class);
    private final co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort destinatarios =
            mock(co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort.class);
    private final ConsultasCacheadasPostProcessor processor = new ConsultasCacheadasPostProcessor(cache, ambito, destinatarios);

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

    /** Verifica cache por usuario: misma respuesta, una sola carga por actor y sin mezclar actores. */
    private <R> void verificaPortalPorUsuario(final Function<AtomicInteger, Object> crear,
                                              final java.util.function.BiFunction<Object, UUID, R> llamar) {
        final AtomicInteger cargas = new AtomicInteger();
        final Object original = crear.apply(cargas);
        final Object envuelto = envolver(original);
        assertNotSame(original, envuelto);

        final R primeraA = llamar.apply(envuelto, ACTOR_A);
        final R segundaA = llamar.apply(envuelto, ACTOR_A);
        final R primeraB = llamar.apply(envuelto, ACTOR_B);

        assertEquals(primeraA, segundaA);
        assertNotEquals(primeraA, primeraB);
        assertEquals(2, cargas.get());
    }

    @Test
    void portal_del_estudiante_y_del_docente_se_cachea_por_usuario() {
        verificaPortalPorUsuario(c -> (ConsultarMateriasEstudianteInputPort) actor -> { c.incrementAndGet(); return List.of(); },
                (b, a) -> ((ConsultarMateriasEstudianteInputPort) b).execute(a) + a.toString());
        verificaPortalPorUsuario(c -> (ConsultarHorariosEstudianteInputPort) actor -> { c.incrementAndGet(); return List.of(); },
                (b, a) -> ((ConsultarHorariosEstudianteInputPort) b).execute(a) + a.toString());
        verificaPortalPorUsuario(c -> (ConsultarPlanEstudioEstudianteInputPort) actor -> { c.incrementAndGet(); return List.of(); },
                (b, a) -> ((ConsultarPlanEstudioEstudianteInputPort) b).execute(a) + a.toString());
        verificaPortalPorUsuario(c -> (ConsultarAsignaturasDocenteInputPort) actor -> { c.incrementAndGet(); return List.of(); },
                (b, a) -> ((ConsultarAsignaturasDocenteInputPort) b).execute(a) + a.toString());
        verificaPortalPorUsuario(c -> (ConsultarHorariosDocenteInputPort) actor -> { c.incrementAndGet(); return List.of(); },
                (b, a) -> ((ConsultarHorariosDocenteInputPort) b).execute(a) + a.toString());
    }

    @Test
    void el_resumen_del_estudiante_se_cachea_y_el_registro_por_qr_lo_invalida() {
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarResumenMateriasEstudianteInputPort resumen = envolver(
                (ConsultarResumenMateriasEstudianteInputPort) actor -> new co.edu.uco.asistenciasuco.application.features.estudiante
                        .consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO(cargas.incrementAndGet(), 0, null, 0));
        final AtomicInteger cargasMaterias = new AtomicInteger();
        final ConsultarMateriasEstudianteInputPort materias = envolver(
                (ConsultarMateriasEstudianteInputPort) actor -> { cargasMaterias.incrementAndGet(); return List.of(); });
        final RegistrarAsistenciaPorTokenInputPort registrar = envolver(
                (RegistrarAsistenciaPorTokenInputPort) comando -> resultado(false));
        final RegistrarAsistenciaPorTokenInputPort repetido = envolver(
                (RegistrarAsistenciaPorTokenInputPort) comando -> resultado(true));

        assertEquals(1, resumen.execute(ACTOR_A).totalMaterias());
        materias.execute(ACTOR_A);
        assertEquals(1, resumen.execute(ACTOR_A).totalMaterias());

        repetido.execute(new RegistrarAsistenciaPorTokenInputPort.Comando("t", null, "10.0.0.1", ACTOR_A));
        assertEquals(1, resumen.execute(ACTOR_A).totalMaterias(), "un registro repetido no invalida");

        registrar.execute(new RegistrarAsistenciaPorTokenInputPort.Comando("t", null, "10.0.0.1", ACTOR_A));
        assertEquals(2, resumen.execute(ACTOR_A).totalMaterias(), "un registro nuevo invalida el resumen");
        materias.execute(ACTOR_A);
        assertEquals(2, cargasMaterias.get(), "un registro nuevo invalida las materias");

        registrar.execute(new RegistrarAsistenciaPorTokenInputPort.Comando("t", null, "10.0.0.1", null));
        assertEquals(2, resumen.execute(ACTOR_A).totalMaterias(), "sin usuario no hay nada que invalidar");
    }

    private static RegistrarAsistenciaPorTokenInputPort.Resultado resultado(final boolean yaRegistrado) {
        return new RegistrarAsistenciaPorTokenInputPort.Resultado(UUID.randomUUID(), UUID.randomUUID(), "Calculo", "G1", 1, 1,
                yaRegistrado, Instant.now(), "ok");
    }

    @Test
    void un_actor_nulo_no_usa_la_cache() {
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarMateriasEstudianteInputPort materias = envolver(
                (ConsultarMateriasEstudianteInputPort) actor -> { cargas.incrementAndGet(); return List.of(); });

        materias.execute(null);
        materias.execute(null);

        assertEquals(2, cargas.get());
    }

    @Test
    void los_indicadores_se_cachean_por_filtro_completo() {
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarKpisCoordinadorInputPort kpis = envolver((ConsultarKpisCoordinadorInputPort) dto -> {
            cargas.incrementAndGet();
            return null;
        });
        final var filtroA = new co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto
                .ConsultarKpisCoordinadorDTO(ACTOR_A, UUID.randomUUID(), null);

        kpis.execute(filtroA);
        kpis.execute(null);

        assertEquals(2, cargas.get(), "un resultado nulo no se guarda y un filtro nulo no usa la cache");
    }

    @Test
    void cada_indicador_queda_envuelto() {
        final List<Object> beans = List.of(
                (ConsultarKpisCoordinadorInputPort) dto -> null,
                (ConsultarAlertaTempranaInputPort) dto -> null,
                (ConsultarKpisDecanoInputPort) dto -> null,
                (ConsultarComparativaProgramasInputPort) dto -> List.of(),
                (ConsultarAforoGruposInputPort) dto -> null,
                (ConsultarKpisInstitucionInputPort) dto -> null,
                (ConsultarComparativaFacultadesInputPort) dto -> List.of(),
                (ConsultarTendenciaAusentismoInputPort) dto -> List.of(),
                (ConsultarTiposIdentificacionInputPort) List::of);
        for (final Object bean : beans) {
            assertNotSame(bean, envolver(bean), bean.getClass().getInterfaces()[0].getSimpleName());
        }
    }

    @Test
    void cada_indicador_consulta_la_fuente_una_vez_por_filtro() {
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarComparativaProgramasInputPort comparativa = envolver((ConsultarComparativaProgramasInputPort) dto -> {
            cargas.incrementAndGet();
            return List.of();
        });
        final ConsultarComparativaFacultadesInputPort facultades = envolver((ConsultarComparativaFacultadesInputPort) dto -> {
            cargas.incrementAndGet();
            return List.of();
        });
        final ConsultarTendenciaAusentismoInputPort tendencia = envolver((ConsultarTendenciaAusentismoInputPort) dto -> {
            cargas.incrementAndGet();
            return List.of();
        });
        final var filtroPrograma = new co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas
                .primaryports.dto.ConsultarComparativaProgramasDTO(ACTOR_A, null);
        final var filtroFacultad = new co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades
                .primaryports.dto.ConsultarComparativaFacultadesDTO(ACTOR_A, null);
        final var filtroMes = new co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo
                .primaryports.dto.ConsultarTendenciaAusentismoDTO(ACTOR_A, null, "MES");
        final var filtroSemana = new co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo
                .primaryports.dto.ConsultarTendenciaAusentismoDTO(ACTOR_A, null, "SEMANA");

        comparativa.execute(filtroPrograma);
        comparativa.execute(filtroPrograma);
        facultades.execute(filtroFacultad);
        facultades.execute(filtroFacultad);
        tendencia.execute(filtroMes);
        tendencia.execute(filtroMes);
        tendencia.execute(filtroSemana);

        assertEquals(4, cargas.get());
    }

    @Test
    void los_catalogos_se_cachean_por_metodo() {
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarCatalogosProgramaInputPort catalogos = envolver(new ConsultarCatalogosProgramaInputPort() {
            @Override
            public List<co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto
                    .CatalogoProgramaDTO> tiposPrograma() {
                cargas.incrementAndGet();
                return List.of();
            }

            @Override
            public List<co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto
                    .CatalogoProgramaDTO> modalidades() {
                cargas.incrementAndGet();
                return List.of();
            }

            @Override
            public List<co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto
                    .CatalogoProgramaDTO> jornadas() {
                cargas.incrementAndGet();
                return List.of();
            }
        });
        final AtomicInteger cargasTipos = new AtomicInteger();
        final ConsultarTiposIdentificacionInputPort tipos = envolver((ConsultarTiposIdentificacionInputPort) () -> {
            cargasTipos.incrementAndGet();
            return List.of();
        });

        for (int i = 0; i < 3; i++) {
            catalogos.tiposPrograma();
            catalogos.modalidades();
            catalogos.jornadas();
            tipos.execute();
        }

        assertEquals(3, cargas.get());
        assertEquals(1, cargasTipos.get());
    }

    interface OtraInterfaz {
    }

    static final class DosContratos implements ConsultarMateriasEstudianteInputPort, OtraInterfaz {
        @Override
        public List<co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO>
                execute(final UUID actorUsuarioId) {
            return List.of();
        }
    }

    @Test
    void no_envuelve_beans_ajenos_ni_los_que_implementan_otros_contratos() {
        final Object ajeno = "texto";
        final DosContratos dos = new DosContratos();

        assertSame(ajeno, envolver(ajeno));
        assertSame(dos, envolver(dos));
    }

    @Test
    void la_cache_se_resuelve_en_la_primera_llamada() {
        @SuppressWarnings("unchecked")
        final ObjectProvider<CacheConsultas> proveedor = mock(ObjectProvider.class);
        when(proveedor.getObject()).thenReturn(cache);
        @SuppressWarnings("unchecked")
        final ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort> sinAmbito =
                mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        final ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort> sinDestinatarios =
                mock(ObjectProvider.class);
        final ConsultasCacheadasPostProcessor perezoso = new ConsultasCacheadasPostProcessor(proveedor, sinAmbito, sinDestinatarios);
        final AtomicInteger cargas = new AtomicInteger();

        final ConsultarHorariosEstudianteInputPort horarios = (ConsultarHorariosEstudianteInputPort) perezoso
                .postProcessAfterInitialization((ConsultarHorariosEstudianteInputPort) actor -> {
                    cargas.incrementAndGet();
                    return List.of();
                }, "horarios");
        horarios.execute(ACTOR_A);
        horarios.execute(ACTOR_A);

        assertEquals(1, cargas.get());
    }

    @Test
    void la_huella_del_filtro_es_estable_y_distingue_filtros() {
        final String a = ConsultasCacheadasPostProcessor.huella(List.of("x", 1));
        assertEquals(a, ConsultasCacheadasPostProcessor.huella(List.of("x", 1)));
        assertNotEquals(a, ConsultasCacheadasPostProcessor.huella(List.of("x", 2)));
        assertEquals(32, a.length());
        assertTrue(ConsultasCacheadasPostProcessor.clavePortal(ACTOR_A, "materias").startsWith(ACTOR_A.toString()));
    }

    // --- Code review 2026-11-26 ---

    private static final java.util.Set<co.edu.uco.asistenciasuco.application.security.InstitutionalRole> COORDINADOR =
            java.util.EnumSet.of(co.edu.uco.asistenciasuco.application.security.InstitutionalRole.COORDINADOR);

    @Test
    void si_el_ambito_del_actor_cambia_el_indicador_vuelve_a_pasar_por_el_caso_de_uso() {
        final UUID programa = UUID.randomUUID();
        when(ambito.findRolesActivosByUsuario(ACTOR_A)).thenReturn(COORDINADOR);
        when(ambito.findProgramaIdByCoordinadorUsuario(ACTOR_A)).thenReturn(java.util.Optional.of(programa));
        when(ambito.canCoordinadorAccessPrograma(ACTOR_A, programa)).thenReturn(true);
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarAlertaTempranaInputPort alertas = envolver((ConsultarAlertaTempranaInputPort) dto -> {
            cargas.incrementAndGet();
            return new co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto
                    .AlertasPaginaDTO(List.of(), 0);
        });
        final var filtro = new co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports
                .dto.ConsultarAlertaTempranaDTO(ACTOR_A, null, null, null, 1, 10, programa);

        alertas.execute(filtro);
        alertas.execute(filtro);
        assertEquals(1, cargas.get(), "con el mismo ambito se usa la cache");

        when(ambito.canCoordinadorAccessPrograma(ACTOR_A, programa)).thenReturn(false);
        alertas.execute(filtro);
        assertEquals(2, cargas.get(), "retirado del programa: no se sirve lo guardado");

        when(ambito.findRolesActivosByUsuario(ACTOR_A)).thenReturn(java.util.Set.of());
        alertas.execute(filtro);
        assertEquals(3, cargas.get(), "sin el rol: no se sirve lo guardado");
    }

    @Test
    void sin_puerto_de_ambito_o_si_falla_los_indicadores_no_usan_la_cache() {
        final ConsultasCacheadasPostProcessor sinAmbito = new ConsultasCacheadasPostProcessor(cache, null, null);
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarKpisDecanoInputPort kpis = (ConsultarKpisDecanoInputPort) sinAmbito.postProcessAfterInitialization(
                (ConsultarKpisDecanoInputPort) dto -> {
                    cargas.incrementAndGet();
                    return null;
                }, "kpis");
        final var filtro = new co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto
                .ConsultarKpisDecanoDTO(ACTOR_A, null);
        kpis.execute(filtro);
        kpis.execute(filtro);
        assertEquals(2, cargas.get());

        when(ambito.findRolesActivosByUsuario(ACTOR_A)).thenThrow(new IllegalStateException("bd"));
        final AtomicInteger cargasDecano = new AtomicInteger();
        final ConsultarComparativaProgramasInputPort comparativa = envolver((ConsultarComparativaProgramasInputPort) dto -> {
            cargasDecano.incrementAndGet();
            return List.of();
        });
        final var filtroDecano = new co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas
                .primaryports.dto.ConsultarComparativaProgramasDTO(ACTOR_A, null);
        comparativa.execute(filtroDecano);
        comparativa.execute(filtroDecano);
        assertEquals(2, cargasDecano.get());
    }

    @Test
    void la_huella_del_ambito_incluye_la_facultad_del_decano() {
        final UUID facultad = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(ambito.findRolesActivosByUsuario(ACTOR_A)).thenReturn(
                java.util.EnumSet.of(co.edu.uco.asistenciasuco.application.security.InstitutionalRole.DECANO));
        when(ambito.findFacultadIdByDecanoUsuario(ACTOR_A)).thenReturn(java.util.Optional.of(facultad));
        when(ambito.canDecanoAccessPrograma(ACTOR_A, programa)).thenReturn(true);

        final String huella = processor.huellaAmbito(ACTOR_A, programa);

        assertTrue(huella.contains(facultad.toString()));
        assertTrue(huella.contains("decanoPrograma=true"));
    }

    private static co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO
            materiaDelGrupo(final UUID grupo) {
        return new co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto
                .MateriaEstudianteDTO(UUID.randomUUID(), "Calculo", grupo, "G1");
    }

    @Test
    void el_resumen_cargado_solo_queda_etiquetado_con_los_grupos_de_las_materias() {
        final UUID grupo = UUID.randomUUID();
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarMateriasEstudianteInputPort materias = envolver(
                (ConsultarMateriasEstudianteInputPort) actor -> List.of(materiaDelGrupo(grupo)));
        final ConsultarResumenMateriasEstudianteInputPort resumen = envolver(
                (ConsultarResumenMateriasEstudianteInputPort) actor -> new co.edu.uco.asistenciasuco.application.features.estudiante
                        .consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO(cargas.incrementAndGet(), 0, null, 0));
        materias.execute(ACTOR_A);
        // Un evento del grupo borra la etiqueta y lo guardado.
        cache.invalidarEtiqueta(RegionCache.PORTAL, ConsultasCacheadasPostProcessor.etiquetaGrupo(grupo));

        assertEquals(1, resumen.execute(ACTOR_A).totalMaterias());
        cache.invalidarEtiqueta(RegionCache.PORTAL, ConsultasCacheadasPostProcessor.etiquetaGrupo(grupo));

        assertEquals(2, resumen.execute(ACTOR_A).totalMaterias(), "el siguiente evento del grupo tambien lo invalida");
    }

    @Test
    void retirar_un_estudiante_invalida_el_portal_del_grupo() {
        final UUID grupo = UUID.randomUUID();
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarMateriasEstudianteInputPort materias = envolver(
                (ConsultarMateriasEstudianteInputPort) actor -> {
                    cargas.incrementAndGet();
                    return List.of(materiaDelGrupo(grupo));
                });
        final co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports
                .RetirarEstudianteGrupoInputPort retirar = envolver(
                (co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports
                        .RetirarEstudianteGrupoInputPort) dto -> { });

        materias.execute(ACTOR_A);
        retirar.execute(new co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto
                .RetirarEstudianteGrupoDTO(ACTOR_B, grupo, UUID.randomUUID(), "motivo"));
        materias.execute(ACTOR_A);

        assertEquals(2, cargas.get());
    }

    @Test
    void resolver_un_reclamo_invalida_el_portal_del_estudiante() {
        final UUID solicitud = UUID.randomUUID();
        when(destinatarios.consultarDestinatariosPorSolicitud(solicitud)).thenReturn(List.of(
                new co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection(
                        "ESTUDIANTE", ACTOR_A, "Ana", "ana@uco.edu.co", UUID.randomUUID(), "G1", "2026-11-26")));
        final AtomicInteger cargas = new AtomicInteger();
        final ConsultarResumenMateriasEstudianteInputPort resumen = envolver(
                (ConsultarResumenMateriasEstudianteInputPort) actor -> new co.edu.uco.asistenciasuco.application.features.estudiante
                        .consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO(cargas.incrementAndGet(), 0, null, 0));
        final co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports
                .ResolverSolicitudRevisionAsistenciaInputPort resolver = envolver(
                (co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports
                        .ResolverSolicitudRevisionAsistenciaInputPort) dto -> { });

        resumen.execute(ACTOR_A);
        resolver.execute(new co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia
                .primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO(solicitud, "APROBADA", "ok", ACTOR_B));

        assertEquals(2, resumen.execute(ACTOR_A).totalMaterias());
    }
}
