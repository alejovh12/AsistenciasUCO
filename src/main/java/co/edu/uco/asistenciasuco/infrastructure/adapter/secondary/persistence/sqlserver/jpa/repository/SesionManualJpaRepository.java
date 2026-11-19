package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): sesiones manuales via {@code usp_consultar_destinatarios_sesion_manual},
 * {@code usp_cancelar_sesion_coordinador} y {@code usp_consultar_motivos_cancelacion_sesion} (JPA, {@code EXEC} nativo). El SP de
 * cancelacion toma {@code Sesion WITH (UPDLOCK, HOLDLOCK)}, igual que {@code usp_cancelar_sesion}.
 */
@Repository
public class SesionManualJpaRepository implements SesionManualRepositoryPort {
    private static final String PARAM_ID_SESION = "idSesion";
    private static final String PARAM_OBSERVACION = "observacion";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String PARAM_SESION = "sesion";

    static final String OP_CANCELAR_COORDINADOR = "cancelarSesionCoordinador";
    static final String SQL_CANCELAR_COORDINADOR = """
            EXEC dbo.usp_cancelar_sesion_coordinador
                 @idSesion = :idSesion, @codigoMotivo = :codigoMotivo, @observacion = :observacion,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_sesion_manual
                 @idSesion = :idSesion, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_APROBAR_SOLICITUD = "aprobarSolicitudSesion";
    static final String SQL_APROBAR_SOLICITUD = """
            EXEC dbo.usp_aprobar_solicitud_sesion
                 @idSesion = :idSesion, @observacion = :observacion,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_RECHAZAR_SOLICITUD = "rechazarSolicitudSesion";
    static final String SQL_RECHAZAR_SOLICITUD = """
            EXEC dbo.usp_rechazar_solicitud_sesion
                 @idSesion = :idSesion, @codigoMotivo = :codigoMotivo, @observacion = :observacion,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESTINATARIOS_SOLICITUD = """
            EXEC dbo.usp_consultar_destinatarios_solicitud_sesion
                 @idSesion = :idSesion, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String PROCEDIMIENTO_SOLICITUDES = "usp_consultar_solicitudes_sesion_coordinador_paginado";
    static final String PROCEDIMIENTO_SOLICITUDES_DOCENTE = "usp_consultar_solicitudes_sesion_docente_paginado";
    static final String SQL_MOTIVOS = """
            EXEC dbo.usp_consultar_motivos_cancelacion_sesion
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SesionManualJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de sesiones manuales es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public SesionCancelada cancelarPorCoordinador(final UUID sesionId, final String codigoMotivo, final String observacion,
                                                  final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_SESION, sesionId);
        parametros.put("codigoMotivo", codigoMotivo);
        parametros.put(PARAM_OBSERVACION, observacion);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, coordinadorUsuarioId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CANCELAR_COORDINADOR, SQL_CANCELAR_COORDINADOR, parametros, correlationId, 4);
        final List<Object> c = fila.extras();
        return new SesionCancelada(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)),
                SesionCancelacionJpaRepository.fechaHora(c.get(2)), c.get(3) == null ? 0 : ((Number) c.get(3)).intValue());
    }

    @Override
    public SolicitudAprobada aprobarSolicitud(final UUID sesionId, final String observacion, final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_SESION, sesionId);
        parametros.put(PARAM_OBSERVACION, observacion);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, coordinadorUsuarioId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_APROBAR_SOLICITUD, SQL_APROBAR_SOLICITUD, parametros, correlationId, 4);
        final List<Object> c = fila.extras();
        return new SolicitudAprobada(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)),
                SesionCancelacionJpaRepository.fechaHora(c.get(2)), c.get(3) == null ? 0 : ((Number) c.get(3)).intValue(),
                fila.result().getMensajeUsuarioResultado());
    }

    @Override
    public SolicitudRechazada rechazarSolicitud(final UUID sesionId, final String codigoMotivo, final String observacion,
                                                final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_SESION, sesionId);
        parametros.put("codigoMotivo", codigoMotivo);
        parametros.put(PARAM_OBSERVACION, observacion);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, coordinadorUsuarioId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_RECHAZAR_SOLICITUD, SQL_RECHAZAR_SOLICITUD, parametros, correlationId, 4);
        final List<Object> c = fila.extras();
        return new SolicitudRechazada(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)),
                SesionCancelacionJpaRepository.fechaHora(c.get(2)), c.get(3) == null ? 0 : ((Number) c.get(3)).intValue(),
                fila.result().getMensajeUsuarioResultado());
    }

    @Override
    public PaginaSolicitudesSesion consultarSolicitudes(final UUID coordinadorUsuarioId, final boolean soloPendientes,
                                                        final UUID programaId, final UUID grupoId, final int pagina, final int tamano) {
        return consultarPaginaSolicitudes("consultarSolicitudesSesionCoordinador", PROCEDIMIENTO_SOLICITUDES,
                parametrosSolicitudes(coordinadorUsuarioId, soloPendientes, programaId, grupoId, pagina, tamano));
    }

    @Override
    public PaginaSolicitudesSesion consultarSolicitudesDocente(final UUID docenteUsuarioId, final boolean soloPendientes,
                                                               final UUID grupoId, final int pagina, final int tamano) {
        return consultarPaginaSolicitudes("consultarSolicitudesSesionDocente", PROCEDIMIENTO_SOLICITUDES_DOCENTE,
                parametrosSolicitudes(docenteUsuarioId, soloPendientes, null, grupoId, pagina, tamano));
    }

    /** {@code programaId} nulo (el docente no filtra por programa) y cualquier otro filtro nulo se omiten del {@code EXEC}. */
    private static Map<String, Object> parametrosSolicitudes(final UUID usuarioId, final boolean soloPendientes, final UUID programaId,
                                                             final UUID grupoId, final int pagina, final int tamano) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idUsuario", usuarioId);
        parametros.put("soloPendientes", soloPendientes ? 1 : 0);
        parametros.put("idPrograma", programaId);
        parametros.put("idGrupo", grupoId);
        parametros.put("numeroPagina", pagina);
        parametros.put("tamanoPagina", tamano);
        return parametros;
    }

    /** Ambos SP de solicitudes devuelven las mismas 20 columnas de {@code uv_solicitud_sesion} y {@code totalRegistros}. */
    private PaginaSolicitudesSesion consultarPaginaSolicitudes(final String operacion, final String procedimiento,
                                                               final Map<String, Object> parametros) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                operacion,
                "No fue posible consultar las solicitudes de sesion extraordinaria.",
                () -> {
                    parametros.put(PARAM_ID_CORRELACION, correlationId);
                    final List<?> filas;
                    try {
                        filas = CancelacionRowSupport.ejecutar(entityManager, procedimiento, parametros);
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, PARAM_SESION);
                    }
                    final List<SolicitudSesion> solicitudes = new ArrayList<>(filas.size());
                    long total = 0;
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        total = Math.max(total, CancelacionRowSupport.entero(c[20]));
                        solicitudes.add(new SolicitudSesion(
                                NativeRowValues.uuid(c[0]), texto(c[1]), texto(c[2]), NativeRowValues.uuid(c[3]), texto(c[4]),
                                texto(c[5]), NativeRowValues.uuid(c[6]), texto(c[7]), NativeRowValues.uuid(c[8]), texto(c[9]),
                                SesionCancelacionJpaRepository.fechaHora(c[10]), SesionCancelacionJpaRepository.fechaHora(c[11]),
                                texto(c[12]), texto(c[13]), SesionCancelacionJpaRepository.fechaHora(c[14]),
                                SesionCancelacionJpaRepository.fechaHora(c[15]), texto(c[16])));
                    }
                    return new PaginaSolicitudesSesion(solicitudes, total);
                });
    }

    @Override
    public List<DestinatarioSesionManual> consultarDestinatariosSolicitud(final UUID sesionId, final UUID coordinadorUsuarioId) {
        return destinatarios("consultarDestinatariosSolicitudSesion",
                "No fue posible consultar los destinatarios de la solicitud de sesion.", SQL_DESTINATARIOS_SOLICITUD, sesionId, coordinadorUsuarioId);
    }

    @Override
    public List<DestinatarioSesionManual> consultarDestinatarios(final UUID sesionId, final UUID docenteUsuarioId) {
        return destinatarios("consultarDestinatariosSesionManual",
                "No fue posible consultar los destinatarios del aviso de la sesion manual.", SQL_DESTINATARIOS, sesionId, docenteUsuarioId);
    }

    private List<DestinatarioSesionManual> destinatarios(final String operacion, final String mensajeError, final String sql,
                                                         final UUID sesionId, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(operacion, mensajeError, () -> {
            final List<?> filas;
            try {
                filas = entityManager.createNativeQuery(sql)
                        .setParameter(PARAM_ID_SESION, sesionId)
                        .setParameter(PARAM_ID_CORRELACION, correlationId)
                        .setParameter(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor)
                        .getResultList();
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception, PARAM_SESION);
            }
            final List<DestinatarioSesionManual> destinatarios = new ArrayList<>(filas.size());
            for (final Object fila : filas) {
                final Object[] c = (Object[]) fila;
                destinatarios.add(new DestinatarioSesionManual(
                        texto(c[0]), NativeRowValues.uuid(c[1]), texto(c[2]), texto(c[3]), texto(c[4]), texto(c[5]),
                        texto(c[6]), SesionCancelacionJpaRepository.fechaHora(c[7]), SesionCancelacionJpaRepository.fechaHora(c[8])));
            }
            return destinatarios;
        });
    }

    @Override
    public List<MotivoCancelacion> consultarMotivosCancelacion(final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarMotivosCancelacionSesion",
                "No fue posible consultar los motivos de cancelacion de sesion.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_MOTIVOS)
                                .setParameter(PARAM_ID_USUARIO_EJECUTOR, coordinadorUsuarioId)
                                .setParameter(PARAM_ID_CORRELACION, correlationId)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, PARAM_SESION);
                    }
                    final List<MotivoCancelacion> motivos = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        motivos.add(new MotivoCancelacion(texto(c[0]), texto(c[1])));
                    }
                    return motivos;
                });
    }

    private static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
