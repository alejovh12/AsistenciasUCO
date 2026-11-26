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
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
 *   <li>Resumen y plan del estudiante (code review 2026-11-26): al cargarse tambien se etiquetan con los grupos de las
 *       materias del estudiante, para que un evento del grupo los invalide aunque materias y horarios no se recarguen.</li>
 *   <li>Ambito en los indicadores (code review 2026-11-26): la clave incluye una huella del ambito vigente del actor
 *       (roles activos, programa del coordinador, facultad del decano y, si el filtro trae programa, si aun tiene acceso a
 *       el). Si el ambito cambia, la clave cambia y la consulta vuelve a pasar por el caso de uso y sus validaciones, en
 *       lugar de servir el resultado guardado hasta el TTL.</li>
 *   <li>Retiro de un estudiante de un grupo y resolucion de un reclamo de asistencia (code review 2026-11-26): no publican
 *       evento realtime, asi que el decorador invalida directamente la etiqueta del grupo o el portal del estudiante.</li>
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultasCacheadasPostProcessor.class);
    /** Indicadores cuyo filtro no trae programa. */
    private static final Function<Object, UUID> SIN_PROGRAMA = filtro -> null;

    private final Supplier<CacheConsultas> cache;
    private final Supplier<InstitutionalScopePort> ambito;
    private final Supplier<DestinatariosReclamoPort> destinatariosReclamo;
    /** Materias del estudiante ya decoradas (se fija al envolver ese bean); sirve para etiquetar resumen y plan. */
    private final AtomicReference<ConsultarMateriasEstudianteInputPort> materiasEstudiante = new AtomicReference<>();

    public ConsultasCacheadasPostProcessor(final ObjectProvider<CacheConsultas> cache,
                                           final ObjectProvider<InstitutionalScopePort> ambito,
                                           final ObjectProvider<DestinatariosReclamoPort> destinatariosReclamo) {
        Objects.requireNonNull(cache, "El proveedor de la cache es obligatorio.");
        Objects.requireNonNull(ambito, "El proveedor del ambito institucional es obligatorio.");
        Objects.requireNonNull(destinatariosReclamo, "El proveedor de destinatarios de reclamos es obligatorio.");
        this.cache = cache::getObject;
        this.ambito = ambito::getIfAvailable;
        this.destinatariosReclamo = destinatariosReclamo::getIfAvailable;
    }

    ConsultasCacheadasPostProcessor(final CacheConsultas cache, final InstitutionalScopePort ambito,
                                    final DestinatariosReclamoPort destinatariosReclamo) {
        Objects.requireNonNull(cache, "La cache es obligatoria.");
        this.cache = () -> cache;
        this.ambito = () -> ambito;
        this.destinatariosReclamo = () -> destinatariosReclamo;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean.getClass().getInterfaces().length != 1) {
            return bean;
        }
        Object envuelto = envolverPortal(bean);
        if (envuelto == null) {
            envuelto = envolverIndicador(bean);
        }
        if (envuelto == null) {
            envuelto = envolverCatalogoOEscritura(bean);
        }
        return envuelto == null ? bean : envuelto;
    }

    /** Portal del estudiante y del docente; {@code null} si el bean no es una de esas consultas. */
    private Object envolverPortal(final Object bean) {
        return switch (bean) {
            case ConsultarMateriasEstudianteInputPort p -> {
                final ConsultarMateriasEstudianteInputPort cacheado = actor ->
                        portal(actor, MATERIAS_ESTUDIANTE, new TypeReference<List<MateriaEstudianteDTO>>() { },
                                () -> p.execute(actor), PORTAL_ESTUDIANTE, MateriaEstudianteDTO::idGrupo);
                materiasEstudiante.set(cacheado);
                yield cacheado;
            }
            case ConsultarHorariosEstudianteInputPort p -> (ConsultarHorariosEstudianteInputPort) actor ->
                    portal(actor, HORARIOS_ESTUDIANTE, new TypeReference<List<HorarioEstudianteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_ESTUDIANTE, HorarioEstudianteDTO::idGrupo);
            case ConsultarResumenMateriasEstudianteInputPort p -> (ConsultarResumenMateriasEstudianteInputPort) actor ->
                    portalEstudianteDerivado(actor, RESUMEN_ESTUDIANTE, new TypeReference<ResumenMateriasEstudianteDTO>() { },
                            () -> p.execute(actor));
            case ConsultarPlanEstudioEstudianteInputPort p -> (ConsultarPlanEstudioEstudianteInputPort) actor ->
                    portalEstudianteDerivado(actor, PLAN_ESTUDIANTE, new TypeReference<List<PlanEstudioEstudianteDTO>>() { },
                            () -> p.execute(actor));
            case ConsultarAsignaturasDocenteInputPort p -> (ConsultarAsignaturasDocenteInputPort) actor ->
                    portal(actor, ASIGNATURAS_DOCENTE, new TypeReference<List<AsignaturaDocenteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_DOCENTE, AsignaturaDocenteDTO::idGrupo);
            case ConsultarHorariosDocenteInputPort p -> (ConsultarHorariosDocenteInputPort) actor ->
                    portal(actor, HORARIOS_DOCENTE, new TypeReference<List<HorarioDocenteDTO>>() { }, () -> p.execute(actor),
                            PORTAL_DOCENTE, HorarioDocenteDTO::idGrupo);
            default -> null;
        };
    }

    /** Indicadores de coordinador, decano y administrador; {@code null} si el bean no es uno de ellos. */
    private Object envolverIndicador(final Object bean) {
        return switch (bean) {
            case ConsultarKpisCoordinadorInputPort p -> (ConsultarKpisCoordinadorInputPort) dto ->
                    indicador("kpis-coordinador", dto, ConsultarKpisCoordinadorDTO::actorUsuarioId,
                            ConsultarKpisCoordinadorDTO::idPrograma, new TypeReference<KpisCoordinadorDTO>() { }, () -> p.execute(dto));
            case ConsultarAlertaTempranaInputPort p -> (ConsultarAlertaTempranaInputPort) dto ->
                    indicador("alertas-tempranas", dto, ConsultarAlertaTempranaDTO::actorUsuarioId,
                            ConsultarAlertaTempranaDTO::idPrograma, new TypeReference<AlertasPaginaDTO>() { }, () -> p.execute(dto));
            case ConsultarKpisDecanoInputPort p -> (ConsultarKpisDecanoInputPort) dto ->
                    indicador("kpis-decano", dto, ConsultarKpisDecanoDTO::actorUsuarioId, SIN_PROGRAMA,
                            new TypeReference<KpisDecanoDTO>() { }, () -> p.execute(dto));
            case ConsultarComparativaProgramasInputPort p -> (ConsultarComparativaProgramasInputPort) dto ->
                    indicador("comparativa-programas", dto, ConsultarComparativaProgramasDTO::actorUsuarioId, SIN_PROGRAMA,
                            new TypeReference<List<ComparativaProgramaDTO>>() { }, () -> p.execute(dto));
            case ConsultarAforoGruposInputPort p -> (ConsultarAforoGruposInputPort) dto ->
                    indicador("aforo-grupos", dto, ConsultarAforoGruposDTO::actorUsuarioId, ConsultarAforoGruposDTO::idPrograma,
                            new TypeReference<AforoGruposPaginaDTO>() { }, () -> p.execute(dto));
            case ConsultarKpisInstitucionInputPort p -> (ConsultarKpisInstitucionInputPort) dto ->
                    indicador("kpis-institucion", dto, ConsultarKpisInstitucionDTO::actorUsuarioId, SIN_PROGRAMA,
                            new TypeReference<KpisInstitucionDTO>() { }, () -> p.execute(dto));
            case ConsultarComparativaFacultadesInputPort p -> (ConsultarComparativaFacultadesInputPort) dto ->
                    indicador("comparativa-facultades", dto, ConsultarComparativaFacultadesDTO::actorUsuarioId, SIN_PROGRAMA,
                            new TypeReference<List<ComparativaFacultadDTO>>() { }, () -> p.execute(dto));
            case ConsultarTendenciaAusentismoInputPort p -> (ConsultarTendenciaAusentismoInputPort) dto ->
                    indicador("tendencia-ausentismo", dto, ConsultarTendenciaAusentismoDTO::actorUsuarioId, SIN_PROGRAMA,
                            new TypeReference<List<PuntoTendenciaDTO>>() { }, () -> p.execute(dto));
            default -> null;
        };
    }

    /** Catalogos y escrituras que invalidan el portal; {@code null} si el bean no es uno de ellos. */
    private Object envolverCatalogoOEscritura(final Object bean) {
        return switch (bean) {
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
            case RetirarEstudianteGrupoInputPort p -> (RetirarEstudianteGrupoInputPort) dto -> {
                p.execute(dto);
                if (dto != null && dto.grupoId() != null) {
                    cache.get().invalidarEtiqueta(RegionCache.PORTAL, etiquetaGrupo(dto.grupoId()));
                }
            };
            case ResolverSolicitudRevisionAsistenciaInputPort p -> (ResolverSolicitudRevisionAsistenciaInputPort) dto -> {
                p.execute(dto);
                if (dto != null && dto.getSolicitud() != null) {
                    invalidarEstudiantesDelReclamo(dto.getSolicitud());
                }
            };
            default -> null;
        };
    }

    /**
     * Resumen y plan del estudiante: sus DTO no traen grupos, asi que antes de consultarlos se obtienen los grupos de las
     * materias del estudiante (normalmente un acierto de cache) y, si hay que cargar, se etiquetan con ellos. Se calcula
     * antes de {@code obtener} para no anidar cargas sobre la misma cache.
     */
    private <T> T portalEstudianteDerivado(final UUID actor, final String consulta, final TypeReference<T> tipo,
                                           final Supplier<T> cargador) {
        if (actor == null) {
            return cargador.get();
        }
        final Set<String> etiquetas = etiquetasDeMaterias(actor);
        final CacheConsultas actual = cache.get();
        return actual.obtener(RegionCache.PORTAL, clavePortal(actor, consulta), tipo, () -> {
            final T valor = cargador.get();
            if (!etiquetas.isEmpty()) {
                actual.etiquetar(RegionCache.PORTAL, etiquetas, claves(actor, PORTAL_ESTUDIANTE));
            }
            return valor;
        });
    }

    private Set<String> etiquetasDeMaterias(final UUID actor) {
        final ConsultarMateriasEstudianteInputPort materias = materiasEstudiante.get();
        if (materias == null) {
            return Set.of();
        }
        try {
            return etiquetasGrupo(materias.execute(actor), MateriaEstudianteDTO::idGrupo);
        } catch (final RuntimeException exception) {
            // Sin grupos conocidos el valor solo vence por TTL; la consulta original decide si el actor puede verla.
            LOGGER.debug("No se pudieron resolver los grupos del estudiante para etiquetar la cache. causa={}",
                    exception.getClass().getSimpleName());
            return Set.of();
        }
    }

    private void invalidarEstudiantesDelReclamo(final UUID solicitud) {
        final DestinatariosReclamoPort puerto = destinatariosReclamo.get();
        if (puerto == null) {
            return;
        }
        try {
            final List<DestinatarioReclamoProjection> destinatarios = puerto.consultarDestinatariosPorSolicitud(solicitud);
            if (destinatarios == null) {
                return;
            }
            for (final DestinatarioReclamoProjection destinatario : destinatarios) {
                if (destinatario != null && destinatario.idUsuario() != null && "ESTUDIANTE".equalsIgnoreCase(destinatario.rol())) {
                    invalidarUsuario(cache.get(), destinatario.idUsuario(), PORTAL_ESTUDIANTE);
                }
            }
        } catch (final RuntimeException exception) {
            LOGGER.debug("No se pudo invalidar el portal tras resolver el reclamo. causa={}", exception.getClass().getSimpleName());
        }
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

    private <F, T> T indicador(final String consulta, final F filtro, final Function<? super F, UUID> actorDe,
                               final Function<? super F, UUID> programaDe, final TypeReference<T> tipo, final Supplier<T> cargador) {
        final UUID actor = filtro == null ? null : actorDe.apply(filtro);
        if (actor == null) {
            return cargador.get();
        }
        final UUID programa = programaDe.apply(filtro);
        final String ambitoVigente;
        try {
            ambitoVigente = huellaAmbito(actor, programa);
        } catch (final RuntimeException _) {
            // Sin poder comprobar el ambito no se usa la cache: el caso de uso valida y responde.
            return cargador.get();
        }
        if (ambitoVigente == null) {
            return cargador.get();
        }
        return cache.get().obtener(RegionCache.INDICADORES, consulta + ":" + huella(filtro + "|" + ambitoVigente), tipo, cargador);
    }

    /**
     * Huella del ambito vigente del actor: roles activos, programa que coordina, facultad que dirige y, si el filtro trae
     * un programa, si todavia puede acceder a el. {@code null} si no hay puerto de ambito (no se cachea).
     */
    String huellaAmbito(final UUID actor, final UUID programa) {
        final InstitutionalScopePort puerto = ambito.get();
        if (puerto == null) {
            return null;
        }
        final Set<InstitutionalRole> roles = puerto.findRolesActivosByUsuario(actor);
        final StringBuilder huella = new StringBuilder("roles=").append(new TreeSet<>(roles == null ? Set.of() : roles));
        if (roles != null && roles.contains(InstitutionalRole.COORDINADOR)) {
            huella.append("|programa=").append(puerto.findProgramaIdByCoordinadorUsuario(actor).orElse(null));
            if (programa != null) {
                huella.append("|accedePrograma=").append(puerto.canCoordinadorAccessPrograma(actor, programa));
            }
        }
        if (roles != null && roles.contains(InstitutionalRole.DECANO)) {
            huella.append("|facultad=").append(puerto.findFacultadIdByDecanoUsuario(actor).orElse(null));
            if (programa != null) {
                huella.append("|decanoPrograma=").append(puerto.canDecanoAccessPrograma(actor, programa));
            }
        }
        return huella.toString();
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
