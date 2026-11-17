package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.MarcaInasistenciaDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatrizAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (F-DOC-004, F-EST-002): resumen por horas de {@code uv_resumen_asistencia_estudiante_grupo} a traves de
 * los SP con ambito. Cada columna se lee por NOMBRE ({@link Tuple}); las columnas de horas que la vista agrega al final no
 * desplazan el mapeo. Horas y porcentajes salen con 2 decimales (HALF_UP).
 */
@Repository
public class ResumenAsistenciaJpaRepository implements ResumenAsistenciaQueryPort, MatrizAsistenciaQueryPort, ResumenSesionesGrupoQueryPort {

    static final String SQL_GRUPO_DOCENTE = """
            EXEC dbo.usp_consultar_resumen_asistencia_grupo_docente_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idGrupo = :idGrupo,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina, @busqueda = :busqueda
            """;
    static final String SQL_MATRIZ = """
            EXEC dbo.usp_consultar_matriz_asistencia_grupo_docente_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idGrupo = :idGrupo,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina, @busqueda = :busqueda
            """;
    static final String SQL_RESUMEN_SESIONES = """
            EXEC dbo.usp_consultar_resumen_sesiones_grupo_docente
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idGrupo = :idGrupo
            """;
    static final String SQL_ESTUDIANTE = """
            EXEC dbo.usp_consultar_resumen_asistencia_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public ResumenAsistenciaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del resumen de asistencia es obligatorio.");
    }

    @Override
    public PaginaResultado<FilaResumen> consultarResumenGrupoDocente(final UUID grupoId, final int numeroPagina, final int tamanoPagina,
                                                                    final String busqueda, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenAsistenciaGrupoDocente",
                "No fue posible consultar el resumen de asistencia del grupo.", () -> {
                    final List<Tuple> filas = tuplas(SQL_GRUPO_DOCENTE, parametrosPagina(usuarioEjecutor, correlacion, grupoId,
                            numeroPagina, tamanoPagina, busqueda));
                    final long total = filas.isEmpty() ? 0 : NativeRowValues.entero(filas.getFirst().get("totalRegistros"));
                    return new PaginaResultado<>(filas.stream().map(f -> aFila(f, NativeRowValues.texto(f.get("nombreCompletoEstudiante")), aMarca(f)))
                            .toList(), numeroPagina, tamanoPagina, total);
                }, excepcion -> DbCodeTranslator.traducir(excepcion, "grupo"));
    }

    private static Map<String, Object> parametrosPagina(final UUID usuarioEjecutor, final UUID correlacion, final UUID grupoId,
                                                        final int numeroPagina, final int tamanoPagina, final String busqueda) {
        final Map<String, Object> parametros = new HashMap<>(Map.of("idUsuarioEjecutor", usuarioEjecutor, "idCorrelacion", correlacion,
                "idGrupo", grupoId, "numeroPagina", numeroPagina, "tamanoPagina", tamanoPagina));
        parametros.put("busqueda", busqueda);
        return parametros;
    }

    /**
     * CIERRE-W3M (punto 1): una fila por (matricula, sesion); se agrupan por matricula conservando el orden del SP (nombre, id) y,
     * dentro de cada una, el de las sesiones (numero). {@code totalRegistros} cuenta estudiantes y {@code idSesion} es NULL en un
     * grupo sin sesiones (la matricula queda sin celdas).
     */
    @Override
    public PaginaResultado<FilaMatriz> consultarMatrizGrupoDocente(final UUID grupoId, final int numeroPagina, final int tamanoPagina,
                                                                  final String busqueda, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarMatrizAsistenciaGrupoDocente",
                "No fue posible consultar la matriz de asistencia del grupo.", () -> {
                    final List<Tuple> filas = tuplas(SQL_MATRIZ, parametrosPagina(usuarioEjecutor, correlacion, grupoId,
                            numeroPagina, tamanoPagina, busqueda));
                    final Map<UUID, FilaMatrizAcumulada> porMatricula = new LinkedHashMap<>();
                    for (final Tuple f : filas) {
                        final FilaMatrizAcumulada acumulada = porMatricula.computeIfAbsent(
                                NativeRowValues.uuid(f.get("idEstudianteGrupo")), id -> new FilaMatrizAcumulada(f));
                        final UUID idSesion = NativeRowValues.uuid(f.get("idSesion"));
                        if (idSesion != null) {
                            acumulada.celdas.add(new CeldaMatriz(idSesion, NativeRowValues.texto(f.get("codigoEstado")),
                                    NativeRowValues.entero(f.get("minutosAsistidos")), NativeRowValues.entero(f.get("minutosSesion"))));
                        }
                    }
                    final long total = filas.isEmpty() ? 0 : NativeRowValues.entero(filas.getFirst().get("totalRegistros"));
                    return new PaginaResultado<>(porMatricula.values().stream().map(FilaMatrizAcumulada::aFila).toList(),
                            numeroPagina, tamanoPagina, total);
                }, excepcion -> DbCodeTranslator.traducir(excepcion, "grupo"));
    }

    /**
     * CIERRE-W3M (puntos 2 y 3): una fila por sesion con las columnas del grupo repetidas; un grupo sin sesiones devuelve una sola
     * fila con {@code idSesion} NULL (solo aporta las columnas del grupo).
     */
    @Override
    public ResumenGrupo consultarResumenSesionesGrupoDocente(final UUID grupoId, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenSesionesGrupoDocente",
                "No fue posible consultar el resumen de sesiones del grupo.", () -> {
                    final List<Tuple> filas = tuplas(SQL_RESUMEN_SESIONES, Map.of("idUsuarioEjecutor", usuarioEjecutor,
                            "idCorrelacion", correlacion, "idGrupo", grupoId));
                    if (filas.isEmpty()) {
                        return new ResumenGrupo(0, 0, 0, 0, null, null, 0, 0, null, null, null, List.of());
                    }
                    final Tuple g = filas.getFirst();
                    final List<SesionResumen> sesiones = filas.stream().filter(f -> f.get("idSesion") != null).map(f -> new SesionResumen(
                            NativeRowValues.uuid(f.get("idSesion")), NativeRowValues.entero(f.get("numero")), NativeRowValues.texto(f.get("nombre")),
                            NativeRowValues.instanteUtc(f.get("fechaHoraInicio")), NativeRowValues.instanteUtc(f.get("fechaHoraFin")),
                            f.get("procedenciaTemporal") != null, NativeRowValues.texto(f.get("estadoSesion")),
                            NativeRowValues.texto(f.get("estadoSolicitud")), NativeRowValues.bit(f.get("pendienteAprobacion")),
                            NativeRowValues.entero(f.get("totalEstudiantes")), NativeRowValues.entero(f.get("presentes")),
                            NativeRowValues.entero(f.get("parciales")), NativeRowValues.entero(f.get("justificadas")),
                            NativeRowValues.entero(f.get("ausentes")), NativeRowValues.entero(f.get("sinRegistro")))).toList();
                    return new ResumenGrupo(NativeRowValues.entero(g.get("sesionesProgramadas")), NativeRowValues.entero(g.get("sesionesEjecutadas")),
                            NativeRowValues.entero(g.get("sesionesCanceladas")), NativeRowValues.entero(g.get("sesionesEnCurso")),
                            NativeRowValues.uuid(g.get("idSesionEnCurso")), NativeRowValues.uuid(g.get("idSesionProxima")),
                            NativeRowValues.entero(g.get("estudiantesActivos")), NativeRowValues.entero(g.get("estudiantesEnRiesgo")),
                            NativeRowValues.decimal2(g.get("promedioAsistencia")), NativeRowValues.decimal2(g.get("umbralAdvertencia")),
                            NativeRowValues.decimal2(g.get("umbralPerdida")), sesiones);
                }, excepcion -> DbCodeTranslator.traducir(excepcion, "grupo"));
    }

    /** Matricula de la matriz en construccion: datos de la primera fila y celdas acumuladas por sesion. */
    private static final class FilaMatrizAcumulada {
        private final UUID idEstudianteGrupo;
        private final UUID idEstudiante;
        private final String nombreCompleto;
        private final String documento;
        private final String codigoEstadoEstudiante;
        private final List<CeldaMatriz> celdas = new ArrayList<>();

        private FilaMatrizAcumulada(final Tuple f) {
            this.idEstudianteGrupo = NativeRowValues.uuid(f.get("idEstudianteGrupo"));
            this.idEstudiante = NativeRowValues.uuid(f.get("idEstudiante"));
            this.nombreCompleto = NativeRowValues.texto(f.get("nombreCompletoEstudiante"));
            this.documento = NativeRowValues.texto(f.get("documento"));
            this.codigoEstadoEstudiante = NativeRowValues.texto(f.get("codigoEstadoEstudiante"));
        }

        private FilaMatriz aFila() {
            return new FilaMatriz(idEstudianteGrupo, idEstudiante, nombreCompleto, documento, codigoEstadoEstudiante, celdas);
        }
    }

    @Override
    public List<FilaResumen> consultarResumenEstudiante(final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenAsistenciaEstudiante",
                "No fue posible consultar el resumen de asistencia del estudiante.", () -> {
                    final List<FilaResumen> resultado = new ArrayList<>();
                    for (final Tuple f : tuplas(SQL_ESTUDIANTE, Map.of("idUsuarioEjecutor", usuarioEjecutor, "idCorrelacion", correlacion))) {
                        resultado.add(aFila(f, null, null));
                    }
                    return resultado;
                }, DbCodeTranslator::traducir);
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final String sql, final Map<String, Object> parametros) {
        final var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        return (List<Tuple>) consulta.getResultList();
    }

    static FilaResumen aFila(final Tuple f, final String nombreCompleto, final MarcaInasistenciaDTO marca) {
        return new FilaResumen(NativeRowValues.uuid(f.get("idGrupo")), NativeRowValues.uuid(f.get("idEstudiante")), nombreCompleto,
                NativeRowValues.texto(f.get("codigoEstadoEstudiante")), new IndicadoresAsistenciaDTO(
                NativeRowValues.entero(f.get("sesionesEjecutadas")),
                NativeRowValues.decimal2(f.get("horasDictadas")),
                NativeRowValues.decimal2(f.get("horasAsistidas")),
                NativeRowValues.decimal2(f.get("horasFalta")),
                NativeRowValues.decimal2(f.get("horasExcusa")),
                NativeRowValues.decimal2(f.get("horasSinRegistro")),
                NativeRowValues.decimal2(f.get("porcentajeInasistencia")),
                NativeRowValues.decimal2(f.get("porcentajeAsistenciaHoras")),
                NativeRowValues.texto(f.get("codigoPermanencia")),
                NativeRowValues.decimal2(f.get("umbralAdvertencia")),
                NativeRowValues.decimal2(f.get("umbralPerdida"))), marca);
    }

    /**
     * CULM-001 C.1: marca de inasistencia (columnas al final del SP del docente); {@code null} sin alertas ni revision.
     * {@code accionesPermitidasRevision} llega como texto separado por comas (vacio si no hay acciones).
     */
    static MarcaInasistenciaDTO aMarca(final Tuple f) {
        final boolean advertencia = NativeRowValues.bit(f.get("tieneAdvertenciaVigente"));
        final boolean perdida = NativeRowValues.bit(f.get("tienePerdidaVigente"));
        final UUID idRevision = NativeRowValues.uuid(f.get("idRevisionCancelacion"));
        if (!advertencia && !perdida && idRevision == null) {
            return null;
        }
        final MarcaInasistenciaDTO.RevisionMarcaDTO revision = idRevision == null ? null : new MarcaInasistenciaDTO.RevisionMarcaDTO(
                idRevision, NativeRowValues.texto(f.get("codigoEstadoRevision")), NativeRowValues.texto(f.get("nombreEstadoRevision")),
                NativeRowValues.bit(f.get("requiereAccionDocente")), NativeRowValues.texto(f.get("codigoPerfilResolutorActual")),
                acciones(NativeRowValues.texto(f.get("accionesPermitidasRevision"))));
        return new MarcaInasistenciaDTO(advertencia, NativeRowValues.instanteUtc(f.get("fechaAdvertenciaUtc")), perdida,
                NativeRowValues.instanteUtc(f.get("fechaPerdidaUtc")), revision);
    }

    private static List<String> acciones(final String texto) {
        return texto == null || texto.isBlank() ? List.of()
                : java.util.Arrays.stream(texto.split(",")).map(String::strip).filter(a -> !a.isEmpty()).toList();
    }
}
