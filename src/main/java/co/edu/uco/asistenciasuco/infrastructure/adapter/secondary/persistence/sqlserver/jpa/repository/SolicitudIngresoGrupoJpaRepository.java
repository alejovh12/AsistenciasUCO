package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * HU053/HU054/HU055: solicitudes de ingreso a un grupo. Los comandos usan el resultset canonico (el de resolver trae
 * ademas {@code codigoEstadoResultante}); las consultas son READ_QUERY_RESULTSET con paginacion en SQL. El ambito
 * (estudiante propietario o docente titular) lo filtra la DB. Los DATETIME2 son instantes UTC (SYSUTCDATETIME()).
 */
@Repository
public class SolicitudIngresoGrupoJpaRepository implements SolicitudIngresoGrupoPort {

    static final String OP_REGISTRAR = "registrarSolicitudIngresoGrupo";
    static final String OP_RESOLVER = "resolverSolicitudIngresoGrupo";
    static final String OP_RETIRAR = "retirarSolicitudIngresoGrupo";
    static final String OP_CONSULTAR_GRUPO = "consultarSolicitudesIngresoGrupoDocente";
    static final String OP_CONSULTAR_ESTUDIANTE = "consultarSolicitudesIngresoEstudiante";
    static final String OP_DESTINATARIOS = "consultarDestinatariosSolicitudIngresoGrupo";

    static final String SQL_REGISTRAR = """
            EXEC dbo.usp_registrar_solicitud_ingreso_grupo
                 @idSolicitud = :idSolicitud,
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_RESOLVER = """
            EXEC dbo.usp_resolver_solicitud_ingreso_grupo
                 @idSolicitud = :idSolicitud,
                 @accion = :accion,
                 @motivo = :motivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_RETIRAR = """
            EXEC dbo.usp_retirar_solicitud_ingreso_grupo
                 @idSolicitud = :idSolicitud,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR_GRUPO = """
            EXEC dbo.usp_consultar_solicitudes_ingreso_grupo_docente
                 @idGrupo = :idGrupo,
                 @codigoEstado = :codigoEstado,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR_ESTUDIANTE = """
            EXEC dbo.usp_consultar_solicitudes_ingreso_estudiante
                 @codigoEstado = :codigoEstado,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_solicitud_ingreso_grupo
                 @idSolicitud = :idSolicitud,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private final JpaProcedureExecutor procedureExecutor;
    private final EntityManager entityManager;

    public SolicitudIngresoGrupoJpaRepository(final JpaProcedureExecutor procedureExecutor, final EntityManager entityManager) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public void registrarSolicitud(final UUID idSolicitud, final UUID idGrupo, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSolicitud", idSolicitud);
        parametros.put("idGrupo", idGrupo);
        final UUID correlationId = comando(parametros, idUsuarioEjecutor);
        procedureExecutor.execute(OP_REGISTRAR, SQL_REGISTRAR, parametros, correlationId);
    }

    @Override
    public String resolverSolicitud(final UUID idSolicitud, final String accion, final String motivo, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSolicitud", idSolicitud);
        parametros.put("accion", accion);
        parametros.put("motivo", motivo);
        final UUID correlationId = comando(parametros, idUsuarioEjecutor);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_RESOLVER, SQL_RESOLVER, parametros, correlationId, 1);
        final Object codigo = fila.extras().getFirst();
        return codigo == null ? null : codigo.toString().strip();
    }

    @Override
    public void retirarSolicitud(final UUID idSolicitud, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSolicitud", idSolicitud);
        final UUID correlationId = comando(parametros, idUsuarioEjecutor);
        procedureExecutor.execute(OP_RETIRAR, SQL_RETIRAR, parametros, correlationId);
    }

    @Override
    public List<SolicitudIngresoDocenteProjection> consultarSolicitudesGrupo(
            final UUID idGrupo, final String codigoEstado, final int numeroPagina, final int tamanoPagina,
            final UUID idUsuarioEjecutor
    ) {
        return consultar(OP_CONSULTAR_GRUPO, "No fue posible consultar las solicitudes de ingreso del grupo desde base de datos.",
                SQL_CONSULTAR_GRUPO, idUsuarioEjecutor, query -> query
                        .setParameter("idGrupo", idGrupo)
                        .setParameter("codigoEstado", codigoEstado)
                        .setParameter("numeroPagina", numeroPagina)
                        .setParameter("tamanoPagina", tamanoPagina),
                fila -> new SolicitudIngresoDocenteProjection(
                        NativeRowValues.uuid(fila[0]), NativeRowValues.uuid(fila[1]), (String) fila[2], entero(fila[3]), texto(fila[4]), (String) fila[5],
                        instante(fila[6]), instante(fila[7]), (String) fila[8], entero(fila[9]),
                        valor(fila[10]), valor(fila[11]), valor(fila[12])));
    }

    @Override
    public List<SolicitudIngresoEstudianteProjection> consultarSolicitudesEstudiante(
            final String codigoEstado, final int numeroPagina, final int tamanoPagina, final UUID idUsuarioEjecutor
    ) {
        return consultar(OP_CONSULTAR_ESTUDIANTE, "No fue posible consultar las solicitudes de ingreso del estudiante desde base de datos.",
                SQL_CONSULTAR_ESTUDIANTE, idUsuarioEjecutor, query -> query
                        .setParameter("codigoEstado", codigoEstado)
                        .setParameter("numeroPagina", numeroPagina)
                        .setParameter("tamanoPagina", tamanoPagina),
                fila -> new SolicitudIngresoEstudianteProjection(
                        NativeRowValues.uuid(fila[0]), NativeRowValues.uuid(fila[1]), entero(fila[2]), (String) fila[3], NativeRowValues.uuid(fila[4]), (String) fila[5],
                        (String) fila[6], texto(fila[7]), (String) fila[8], instante(fila[9]), instante(fila[10]),
                        (String) fila[11], valor(fila[12])));
    }

    @Override
    public List<DestinatarioSolicitudIngresoProjection> consultarDestinatarios(final UUID idSolicitud, final UUID idUsuarioEjecutor) {
        return consultar(OP_DESTINATARIOS, "No fue posible consultar los destinatarios de la solicitud de ingreso desde base de datos.",
                SQL_DESTINATARIOS, idUsuarioEjecutor, query -> query.setParameter("idSolicitud", idSolicitud),
                fila -> new DestinatarioSolicitudIngresoProjection(
                        texto(fila[0]), NativeRowValues.uuid(fila[1]), (String) fila[2], (String) fila[3], NativeRowValues.uuid(fila[4]), entero(fila[5]),
                        (String) fila[6], (String) fila[7]));
    }

    private static UUID comando(final Map<String, Object> parametros, final UUID idUsuarioEjecutor) {
        if (ObjectHelper.isNull(idUsuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la solicitud de ingreso es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);
        return correlationId;
    }

    private <T> List<T> consultar(
            final String operacion,
            final String mensajeError,
            final String sql,
            final UUID idUsuarioEjecutor,
            final Function<Query, Query> parametros,
            final Function<Object[], T> mapeo
    ) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(operacion, mensajeError, () -> ((List<?>) parametros.apply(
                        entityManager.createNativeQuery(sql))
                .setParameter("idCorrelacion", correlationId)
                .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                .getResultList()).stream()
                .map(fila -> mapeo.apply((Object[]) fila))
                .toList());
    }

    /**
     * DATETIME2 UTC del SP. El driver puede entregarlo como LocalDateTime o como java.util.Date (Timestamp, construido
     * con la zona de la JVM): se recupera la fecha/hora de pared sin depender de java.sql y se interpreta como UTC.
     */
    private static Instant instante(final Object valor) {
        if (valor instanceof LocalDateTime fecha) {
            return SoporteArchivoJpaRepository.toUtcInstant(fecha);
        }
        if (valor instanceof Date fecha) {
            return SoporteArchivoJpaRepository.toUtcInstant(LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault()));
        }
        return null;
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static int valor(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
