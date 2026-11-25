package co.edu.uco.asistenciasuco.infrastructure.cache.decorador;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.ConsultarPlanEstudioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.ConsultarResumenMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import tools.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * REDIS-001: aplica la cache de consultas (cache-aside) a las lecturas mas repetidas sin tocar Application ni los
 * {@code *WiringConfiguration}: envuelve el bean del InputPort con un decorador del mismo contrato.
 *
 * <ul>
 *   <li>{@link RegionCache#PORTAL}: materias, horarios, resumen y plan del estudiante; asignaturas y horarios del docente.
 *       Clave {@code <usuario>:<consulta>}: nunca se comparte el resultado de un usuario con otro.</li>
 *   <li>{@link RegionCache#INDICADORES}: KPIs, alertas tempranas, comparativas, aforo y tendencia. Clave: hash del
 *       filtro completo (incluye al actor, el periodo, el programa y la pagina), asi el alcance institucional se conserva.</li>
 *   <li>{@link RegionCache#CATALOGOS}: tipos de programa, modalidades, jornadas y tipos de identificacion (solo consulta).</li>
 *   <li>Invalidacion: un registro por QR nuevo borra las materias y el resumen del estudiante que lo hizo; una matricula
 *       por QR borra todo el portal del estudiante. Al cargar materias u horarios, las claves del usuario quedan
 *       etiquetadas con {@code grupo:<id>} de cada grupo del resultado; {@link InvalidacionCachePorEventos} borra esa
 *       etiqueta cuando el docente cambia asistencias, cierra una toma, cancela una sesion o se matricula alguien.</li>
 * </ul>
 *
 * <p>La cache se resuelve en la primera llamada (no al crear el post-processor) para no adelantar la creacion de sus
 * dependencias. Solo se envuelve un bean que implementa exactamente ese contrato, para no ocultar otras interfaces.</p>
 */
public final class ConsultasCacheadasPostProcessor implements BeanPostProcessor {

    static final String MATERIAS_ESTUDIANTE = "materias-estudiante";
    static final String HORARIOS_ESTUDIANTE = "horarios-estudiante";
    static final String RESUMEN_ESTUDIANTE = "resumen-estudiante";
    static final String PLAN_ESTUDIANTE = "plan-estudiante";
    static final String ASIGNATURAS_DOCENTE = "asignaturas-docente";
    static final String HORARIOS_DOCENTE = "horarios-docente";
    /** Consultas del portal que dependen de los grupos del estudiante: se borran juntas. */
    static final List<String> PORTAL_ESTUDIANTE = List.of(MATERIAS_ESTUDIANTE, HORARIOS_ESTUDIANTE, RESUMEN_ESTUDIANTE,
            PLAN_ESTUDIANTE);
    /** Consultas del portal del docente que dependen de sus grupos. */
    static final List<String> PORTAL_DOCENTE = List.of(ASIGNATURAS_DOCENTE, HORARIOS_DOCENTE);

    private final Supplier<CacheConsultas> cache;

    public ConsultasCacheadasPostProcessor(final ObjectProvider<CacheConsultas> cache) {
        Objects.requireNonNull(cache, "El proveedor de la cache es obligatorio.");
        this.cache = cache::getObject;
    }

    ConsultasCacheadasPostProcessor(final CacheConsultas cache) {
        Objects.requireNonNull(cache, "La cache es obligatoria.");
        this.cache = () -> cache;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean.getClass().getInterfaces().length != 1) {
            return bean;
        }
        return switch (bean) {
            case ConsultarMateriasEstudianteInputPort p -> (ConsultarMateriasEstudianteInputPort) actor ->
                    portal(actor, MATERIAS_ESTUDIANTE, new TypeReference<List<MateriaEstudianteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_ESTUDIANTE, MateriaEstudianteDTO::idGrupo);
            case ConsultarHorariosEstudianteInputPort p -> (ConsultarHorariosEstudianteInputPort) actor ->
                    portal(actor, HORARIOS_ESTUDIANTE, new TypeReference<List<HorarioEstudianteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_ESTUDIANTE, HorarioEstudianteDTO::idGrupo);
            case ConsultarResumenMateriasEstudianteInputPort p -> (ConsultarResumenMateriasEstudianteInputPort) actor ->
                    portal(actor, RESUMEN_ESTUDIANTE, new TypeReference<ResumenMateriasEstudianteDTO>() { }, () -> p.execute(actor));
            case ConsultarPlanEstudioEstudianteInputPort p -> (ConsultarPlanEstudioEstudianteInputPort) actor ->
                    portal(actor, PLAN_ESTUDIANTE, new TypeReference<List<PlanEstudioEstudianteDTO>>() { }, () -> p.execute(actor));
            case ConsultarAsignaturasDocenteInputPort p -> (ConsultarAsignaturasDocenteInputPort) actor ->
                    portal(actor, ASIGNATURAS_DOCENTE, new TypeReference<List<AsignaturaDocenteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_DOCENTE, AsignaturaDocenteDTO::idGrupo);
            case ConsultarHorariosDocenteInputPort p -> (ConsultarHorariosDocenteInputPort) actor ->
                    portal(actor, HORARIOS_DOCENTE, new TypeReference<List<HorarioDocenteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_DOCENTE, HorarioDocenteDTO::idGrupo);
            case ConsultarKpisCoordinadorInputPort p -> (ConsultarKpisCoordinadorInputPort) dto ->
                    indicador("kpis-coordinador", dto, new TypeReference<KpisCoordinadorDTO>() { }, () -> p.execute(dto));
            case ConsultarAlertaTempranaInputPort p -> (ConsultarAlertaTempranaInputPort) dto ->
                    indicador("alertas-tempranas", dto, new TypeReference<AlertasPaginaDTO>() { }, () -> p.execute(dto));
            case ConsultarKpisDecanoInputPort p -> (ConsultarKpisDecanoInputPort) dto ->
                    indicador("kpis-decano", dto, new TypeReference<KpisDecanoDTO>() { }, () -> p.execute(dto));
            case ConsultarComparativaProgramasInputPort p -> (ConsultarComparativaProgramasInputPort) dto ->
                    indicador("comparativa-programas", dto, new TypeReference<List<ComparativaProgramaDTO>>() { }, () -> p.execute(dto));
            case ConsultarAforoGruposInputPort p -> (ConsultarAforoGruposInputPort) dto ->
                    indicador("aforo-grupos", dto, new TypeReference<AforoGruposPaginaDTO>() { }, () -> p.execute(dto));
            case ConsultarKpisInstitucionInputPort p -> (ConsultarKpisInstitucionInputPort) dto ->
                    indicador("kpis-institucion", dto, new TypeReference<KpisInstitucionDTO>() { }, () -> p.execute(dto));
            case ConsultarComparativaFacultadesInputPort p -> (ConsultarComparativaFacultadesInputPort) dto ->
                    indicador("comparativa-facultades", dto, new TypeReference<List<ComparativaFacultadDTO>>() { }, () -> p.execute(dto));
            case ConsultarTendenciaAusentismoInputPort p -> (ConsultarTendenciaAusentismoInputPort) dto ->
                    indicador("tendencia-ausentismo", dto, new TypeReference<List<PuntoTendenciaDTO>>() { }, () -> p.execute(dto));
            case ConsultarTiposIdentificacionInputPort p -> (ConsultarTiposIdentificacionInputPort) () ->
                    cache.get().obtener(RegionCache.CATALOGOS, "tipos-identificacion",
                            new TypeReference<List<TipoIdentificacionDTO>>() { }, p::execute);
            case ConsultarCatalogosProgramaInputPort p -> new CatalogosProgramaCacheados(p);
            case RegistrarAsistenciaPorTokenInputPort p -> (RegistrarAsistenciaPorTokenInputPort) comando -> {
                final RegistrarAsistenciaPorTokenInputPort.Resultado resultado = p.execute(comando);
                if (resultado != null && !resultado.yaRegistrado() && comando != null && comando.usuarioEjecutor() != null) {
                    invalidarAsistenciaEstudiante(comando.usuarioEjecutor());
                }
                return resultado;
            };
            case MatricularEstudianteAutenticadoInputPort p -> (MatricularEstudianteAutenticadoInputPort) comando -> {
                final MatricularEstudianteAutenticadoInputPort.Resultado resultado = p.execute(comando);
                if (comando != null && comando.usuarioEjecutor() != null) {
                    invalidarUsuario(cache.get(), comando.usuarioEjecutor(), PORTAL_ESTUDIANTE);
                }
                return resultado;
            };
            default -> bean;
        };
    }

    private <T> T portal(final UUID actor, final String consulta, final TypeReference<T> tipo, final Supplier<T> cargador) {
        if (actor == null) {
            return cargador.get();
        }
        return cache.get().obtener(RegionCache.PORTAL, clavePortal(actor, consulta), tipo, cargador);
    }

    /**
     * Portal con etiquetas: en un fallo de cache, tras cargar, asocia las claves {@code consultasDelUsuario} del actor a
     * {@code grupo:<id>} de cada grupo del resultado. Solo se etiqueta al cargar (no en cada acierto).
     */
    private <E> List<E> portal(final UUID actor, final String consulta, final TypeReference<List<E>> tipo,
                               final Supplier<List<E>> cargador, final List<String> consultasDelUsuario,
                               final Function<E, UUID> grupoDe) {
        if (actor == null) {
            return cargador.get();
        }
        final CacheConsultas actual = cache.get();
        return actual.obtener(RegionCache.PORTAL, clavePortal(actor, consulta), tipo, () -> {
            final List<E> valor = cargador.get();
            final Set<String> etiquetas = etiquetasGrupo(valor, grupoDe);
            if (!etiquetas.isEmpty()) {
                actual.etiquetar(RegionCache.PORTAL, etiquetas, claves(actor, consultasDelUsuario));
            }
            return valor;
        });
    }

    private static <E> Set<String> etiquetasGrupo(final List<E> valor, final Function<E, UUID> grupoDe) {
        final Set<String> etiquetas = new LinkedHashSet<>();
        if (valor != null) {
            for (final E elemento : valor) {
                final UUID grupo = elemento == null ? null : grupoDe.apply(elemento);
                if (grupo != null) {
                    etiquetas.add(etiquetaGrupo(grupo));
                }
            }
        }
        return etiquetas;
    }

    private static List<String> claves(final UUID usuario, final Collection<String> consultas) {
        return consultas.stream().map(consulta -> clavePortal(usuario, consulta)).toList();
    }

    static void invalidarUsuario(final CacheConsultas cache, final UUID usuario, final Collection<String> consultas) {
        for (final String clave : claves(usuario, consultas)) {
            cache.invalidar(RegionCache.PORTAL, clave);
        }
    }

    static String etiquetaGrupo(final UUID grupo) {
        return "grupo:" + grupo;
    }

    private <T> T indicador(final String consulta, final Object filtro, final TypeReference<T> tipo, final Supplier<T> cargador) {
        if (filtro == null) {
            return cargador.get();
        }
        return cache.get().obtener(RegionCache.INDICADORES, consulta + ":" + huella(filtro), tipo, cargador);
    }

    private void invalidarAsistenciaEstudiante(final UUID usuario) {
        final CacheConsultas actual = cache.get();
        actual.invalidar(RegionCache.PORTAL, clavePortal(usuario, MATERIAS_ESTUDIANTE));
        actual.invalidar(RegionCache.PORTAL, clavePortal(usuario, RESUMEN_ESTUDIANTE));
    }

    static String clavePortal(final UUID actor, final String consulta) {
        return actor + ":" + consulta;
    }

    /** Huella estable del filtro: los DTO de consulta son records, cuyo {@code toString} incluye todos sus componentes. */
    static String huella(final Object filtro) {
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(filtro.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM.", exception);
        }
    }

    private final class CatalogosProgramaCacheados implements ConsultarCatalogosProgramaInputPort {

        private static final TypeReference<List<CatalogoProgramaDTO>> TIPO = new TypeReference<>() { };
        private final ConsultarCatalogosProgramaInputPort delegado;

        private CatalogosProgramaCacheados(final ConsultarCatalogosProgramaInputPort delegado) {
            this.delegado = delegado;
        }

        @Override
        public List<CatalogoProgramaDTO> tiposPrograma() {
            return cache.get().obtener(RegionCache.CATALOGOS, "tipos-programa", TIPO, delegado::tiposPrograma);
        }

        @Override
        public List<CatalogoProgramaDTO> modalidades() {
            return cache.get().obtener(RegionCache.CATALOGOS, "modalidades-programa", TIPO, delegado::modalidades);
        }

        @Override
        public List<CatalogoProgramaDTO> jornadas() {
            return cache.get().obtener(RegionCache.CATALOGOS, "jornadas-programa", TIPO, delegado::jornadas);
        }
    }
}
