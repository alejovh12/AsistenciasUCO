package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.TomaSesionEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ResultadoAprobacionReclamo;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper.ConsultarSesionesMateriaEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaTomaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MinutosSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sesiones de una materia del estudiante autenticado, con su estado real de asistencia.
 * El estudiante se resuelve en el servidor desde el usuario autenticado. Una sesion sin cabecera o sin detalle de
 * asistencia es {@code NO_DISPONIBLE}: jamas se asume ausencia por falta de registro.
 * CULM-001 D.1 (HU026B): las sesiones salen de {@code usp_consultar_sesiones_materia_estudiante}, que valida el ambito
 * (matricula inscrita del usuario), pagina y trae la fecha limite de justificacion y si el plazo sigue vigente; este caso de uso
 * solo los transporta. Una materia tiene menos sesiones que PAGINACION/TAMANO_MAXIMO; si el total lo supera se registra un
 * aviso y se devuelve la primera pagina (deuda a revisar en CLOSURE).
 */
public final class ConsultarSesionesMateriaEstudianteUseCaseImpl implements ConsultarSesionesMateriaEstudianteUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarSesionesMateriaEstudianteUseCaseImpl.class);
    static final String GRUPO_PAGINACION = "PAGINACION";
    static final String CLAVE_TAMANO_MAXIMO = "TAMANO_MAXIMO";
    /** Valor de la semilla PAGINACION/TAMANO_MAXIMO; solo se usa si el catalogo no entrega un entero positivo. */
    static final int TAMANO_MAXIMO_SEMILLA = 100;
    private static final String RAZON_ASISTENCIA_NORMAL = "AN";
    private static final String RAZON_SIN_JUSTA_CAUSA = "SJC";
    private static final String RAZON_EXCUSA = "EX";
    private static final String SOLICITUD_ESTADO_DESCONOCIDO = "DESCONOCIDO";

    private final InstitutionalScopePort scopePort;
    private final SesionMateriaEstudianteQueryPort queryPort;
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort;
    private final SesionProcedenciaQueryPort procedenciaPort;
    private final ParameterCatalogPort parametros;

    public ConsultarSesionesMateriaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final SesionMateriaEstudianteQueryPort queryPort,
                                                         final AsistenciaSesionEstudianteQueryPort asistenciaPort,
                                                         final SesionProcedenciaQueryPort procedenciaPort,
                                                         final ParameterCatalogPort parametros) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.asistenciaPort = Objects.requireNonNull(asistenciaPort);
        this.procedenciaPort = Objects.requireNonNull(procedenciaPort, "SesionProcedenciaQueryPort es obligatorio.");
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
    }

    @Override public List<SesionMateriaEstudianteDomain> execute(final UUID actorUsuarioId, final UUID idAsignatura,
                                                                 final Integer pagina, final Integer tamano) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        final int numeroPagina = pagina == null ? 1 : pagina;
        final int tamanoPagina = tamano == null ? tamanoPorDefecto() : tamano;
        final PaginaResultado<SesionMateriaEstudianteProjection> resultado =
                queryPort.consultarSesionesMateria(actorUsuarioId, idAsignatura, numeroPagina, tamanoPagina);
        if (resultado.totalElementos() > (long) resultado.elementos().size() + (long) (numeroPagina - 1) * tamanoPagina
                && pagina == null && tamano == null) {
            LOGGER.warn("La materia {} del estudiante tiene {} sesiones y se devuelve solo la primera pagina de {}.",
                    idAsignatura, resultado.totalElementos(), tamanoPagina);
        }
        final List<SesionMateriaEstudianteProjection> sesiones = resultado.elementos();
        if (sesiones.isEmpty()) {
            return List.of();
        }
        // Lecturas complementarias limitadas a las sesiones de la pagina, sin consultar la materia ni el grupo completos.
        final Set<UUID> idsPagina = sesiones.stream().map(SesionMateriaEstudianteProjection::id)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        final Map<UUID, List<AsistenciaSesionEstudianteProjection>> hechosPorSesion = new HashMap<>();
        for (final var hecho : asistenciaPort.consultarAsistenciaSesiones(idEstudiante, idAsignatura, idsPagina)) {
            if (idsPagina.contains(hecho.idSesion())) {
                hechosPorSesion.computeIfAbsent(hecho.idSesion(), k -> new ArrayList<>()).add(hecho);
            }
        }
        final Map<UUID, SesionProcedenciaEntity> procedencias = procedencias(sesiones, idsPagina);
        final Map<UUID, List<TomaSesionEstudianteDomain>> tomasPorSesion = tomasPorSesion(
                asistenciaPort.consultarTomasSesiones(idEstudiante, idAsignatura, idsPagina), idsPagina);
        final Map<UUID, MinutosSesionEstudianteProjection> minutosPorSesion = new HashMap<>();
        for (final var minutos : asistenciaPort.consultarMinutosSesiones(idEstudiante, idAsignatura, idsPagina)) {
            if (idsPagina.contains(minutos.idSesion())) {
                minutosPorSesion.putIfAbsent(minutos.idSesion(), minutos);
            }
        }
        return sesiones.stream()
                .map(sesion -> {
                    final var hechos = hechosPorSesion.getOrDefault(sesion.id(), List.of());
                    final SesionProcedenciaEntity procedencia = procedencias.get(sesion.id());
                    final var tomas = tomasPorSesion.getOrDefault(sesion.id(), List.of());
                    final var minutos = minutosPorSesion.get(sesion.id());
                    // CULM-001 B.1 (3.2.4): una sesion cancelada no tiene asistencia que mostrar ni es reclamable.
                    final boolean cancelada = procedencia != null && procedencia.cancelacion() != null;
                    // PC (puntos de control): el estado sale de la base de datos (minutos por tramo); sin fila se usa el respaldo por marcas.
                    final EstadoAsistenciaSesion estado = cancelada ? EstadoAsistenciaSesion.NO_DISPONIBLE : estadoDeLaBaseDeDatos(minutos, hechos);
                    final String estadoSolicitud = resolverEstadoSolicitud(hechos);
                    // PC: reclamable si alguna toma lo es (la base de datos valida ausencia, solicitud previa y plazo); sin tomas, la regla anterior.
                    final boolean reclamable = !cancelada && (tomas.isEmpty()
                            ? (estado == EstadoAsistenciaSesion.AUSENTE || estado == EstadoAsistenciaSesion.PARCIAL) && estadoSolicitud == null
                            : tomas.stream().anyMatch(TomaSesionEstudianteDomain::reclamable));
                    return ConsultarSesionesMateriaEstudianteRepositoryMapper.toDomain(
                            sesion, procedencia, estado, estadoSolicitud, reclamable, cancelada ? null : minutos, cancelada ? List.of() : tomas);
                })
                .toList();
    }

    private static EstadoAsistenciaSesion estadoDeLaBaseDeDatos(final MinutosSesionEstudianteProjection minutos,
                                                                final List<AsistenciaSesionEstudianteProjection> hechos) {
        if (minutos != null && minutos.codigoEstadoSesion() != null) {
            try {
                return EstadoAsistenciaSesion.valueOf(minutos.codigoEstadoSesion().strip().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ignorado) {
                LOGGER.warn("Estado de sesion desconocido {}; se usa el respaldo por marcas.", minutos.codigoEstadoSesion());
            }
        }
        return resolverEstado(hechos);
    }

    private static Map<UUID, List<TomaSesionEstudianteDomain>> tomasPorSesion(
            final List<AsistenciaTomaEstudianteProjection> filas, final Set<UUID> idsPagina) {
        final Map<UUID, List<TomaSesionEstudianteDomain>> resultado = new HashMap<>();
        for (final AsistenciaTomaEstudianteProjection f : filas) {
            if (idsPagina.contains(f.idSesion())) {
                resultado.computeIfAbsent(f.idSesion(), k -> new ArrayList<>()).add(new TomaSesionEstudianteDomain(f.idToma(),
                        f.numeroToma(), f.fechaPuntoControlUtc(), f.codigoEstadoToma(), f.esReclamable(), f.idSolicitud(),
                        nombreEstadoSolicitud(f.codigoEstadoSolicitud()),
                        ResultadoAprobacionReclamo.nombreDesdeCodigo(f.codigoAprobacion())));
            }
        }
        return resultado;
    }

    /** Codigos de {@code dbo.Estado} de la solicitud: P pendiente, A aprobada, R rechazada; otro valor se transporta tal cual. */
    private static String nombreEstadoSolicitud(final String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return switch (codigo.strip().toUpperCase(Locale.ROOT)) {
            case "P" -> "PENDIENTE";
            case "A" -> "APROBADA";
            case "R" -> "RECHAZADA";
            default -> codigo.strip();
        };
    }

    /** PAGINACION/TAMANO_MAXIMO del catalogo; ausente, vacio o mal formado vale 100 (semilla). */
    private int tamanoPorDefecto() {
        try {
            final int valor = parametros.getParameter(GRUPO_PAGINACION, CLAVE_TAMANO_MAXIMO)
                    .map(String::strip).filter(texto -> !texto.isEmpty()).map(Integer::parseInt).orElse(TAMANO_MAXIMO_SEMILLA);
            return valor < 1 ? TAMANO_MAXIMO_SEMILLA : valor;
        } catch (final NumberFormatException | ParameterCatalogPort.ParameterCatalogException exception) {
            return TAMANO_MAXIMO_SEMILLA;
        }
    }

    /**
     * APP-04: misma regla que HU060 coordinador. Las sesiones ya vienen aisladas por estudiante/asignatura; la
     * procedencia se consulta una sola vez, solo para las sesiones de la pagina, y se aplica unicamente a filas con el
     * mismo id y el mismo grupo que la sesion de la pagina. Sin procedencia CONFIRMADA no se ofrecen instantes UTC
     * (estadoTemporal INDETERMINADA).
     */
    private Map<UUID, SesionProcedenciaEntity> procedencias(final List<SesionMateriaEstudianteProjection> sesiones,
                                                            final Set<UUID> idsPagina) {
        final Map<UUID, UUID> grupoPorSesion = new HashMap<>();
        sesiones.stream().filter(sesion -> sesion.id() != null && sesion.idGrupo() != null)
                .forEach(sesion -> grupoPorSesion.putIfAbsent(sesion.id(), sesion.idGrupo()));
        final Map<UUID, SesionProcedenciaEntity> resultado = new HashMap<>();
        if (grupoPorSesion.isEmpty()) {
            return resultado;
        }
        procedenciaPort.consultarSesionesPorIds(idsPagina).stream()
                .map(SesionProcedenciaMapper::toEntity)
                .filter(p -> p.grupo() != null && p.grupo().equals(grupoPorSesion.get(p.sesion())))
                .forEach(p -> resultado.putIfAbsent(p.sesion(), p));
        return resultado;
    }

    /**
     * Estado consolidado de la sesion con todas sus tomas, segun CULM-001 B.2 [SUPUESTO-B-15]: si alguna toma es
     * {@code SJC} la sesion es AUSENTE (o PARCIAL si otra toma es {@code AN}); si no, JUSTIFICADA si alguna es
     * {@code EX}; si no, PRESENTE si alguna es {@code AN}. Sin detalle o con parejas no definidas, NO_DISPONIBLE.
     *
     * <p>CERT-CULM001: antes se tomaba solo el detalle de mayor codigo (la ultima toma escrita), asi que una sesion con
     * una toma SJC y otra AN se mostraba PRESENTE y no se podia reclamar, mientras la planilla del docente mostraba SJC.</p>
     */
    static EstadoAsistenciaSesion resolverEstado(final List<AsistenciaSesionEstudianteProjection> hechos) {
        final Set<EstadoAsistenciaSesion> porToma = hechos.stream()
                .filter(h -> h.asistio() != null)
                .map(h -> clasificar(h.asistio(), h.codigoRazonCausa()))
                .collect(Collectors.toSet());
        if (porToma.contains(EstadoAsistenciaSesion.AUSENTE)) {
            return porToma.contains(EstadoAsistenciaSesion.PRESENTE)
                    ? EstadoAsistenciaSesion.PARCIAL : EstadoAsistenciaSesion.AUSENTE;
        }
        if (porToma.contains(EstadoAsistenciaSesion.JUSTIFICADA)) {
            return EstadoAsistenciaSesion.JUSTIFICADA;
        }
        if (porToma.contains(EstadoAsistenciaSesion.PRESENTE)) {
            return EstadoAsistenciaSesion.PRESENTE;
        }
        return EstadoAsistenciaSesion.NO_DISPONIBLE;
    }

    private static EstadoAsistenciaSesion clasificar(final boolean asistio, final String codigoRazonCausa) {
        final String razon = codigoRazonCausa == null ? "" : codigoRazonCausa.trim().toUpperCase(Locale.ROOT);
        if (asistio) {
            return RAZON_ASISTENCIA_NORMAL.equals(razon)
                    ? EstadoAsistenciaSesion.PRESENTE : EstadoAsistenciaSesion.NO_DISPONIBLE;
        }
        return switch (razon) {
            case RAZON_SIN_JUSTA_CAUSA -> EstadoAsistenciaSesion.AUSENTE;
            case RAZON_EXCUSA -> EstadoAsistenciaSesion.JUSTIFICADA;
            default -> EstadoAsistenciaSesion.NO_DISPONIBLE;
        };
    }

    /**
     * Estado de la solicitud mas reciente, o nulo si la asistencia no tiene ninguna. Cualquier solicitud existente
     * (pendiente, aceptada o rechazada) impide ofrecer un nuevo reclamo desde la UI.
     */
    private static String resolverEstadoSolicitud(final List<AsistenciaSesionEstudianteProjection> hechos) {
        return hechos.stream()
                .filter(h -> h.idSolicitud() != null)
                .max(Comparator.comparing(h -> h.fechaSolicitud() == null ? LocalDate.MIN : h.fechaSolicitud()))
                .map(h -> h.estadoSolicitud() == null || h.estadoSolicitud().isBlank()
                        ? SOLICITUD_ESTADO_DESCONOCIDO : h.estadoSolicitud())
                .orElse(null);
    }
}
