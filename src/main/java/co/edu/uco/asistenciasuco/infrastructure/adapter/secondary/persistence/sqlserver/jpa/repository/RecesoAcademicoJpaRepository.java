package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * DEC-081 (migracion 68): receso academico del periodo via {@code usp_crear_receso_academico} (tupla canonica con {@code idReceso},
 * {@code sesionesCanceladas} y {@code sesionesConAsistencia}),
 * {@code usp_retirar_receso_academico}, {@code usp_consultar_recesos_periodo} y {@code usp_consultar_destinatarios_receso_academico}.
 * Las lecturas rechazan con {@code THROW 51000} y {@code DBCODE=...} (SEC_001, PER_001, REC_009): {@link DbCodeTranslator} las
 * convierte en su error HTTP.
 */
@Repository
public class RecesoAcademicoJpaRepository implements RecesoAcademicoRepositoryPort {

    static final String OP_CREAR = "crearRecesoAcademico";
    /**
     * {@code @incluirDetalle = 0}: solo la tupla canonica. El segundo conjunto (sesiones canceladas) no se lee porque un
     * {@code StoredProcedureQuery} sobre el {@code EntityManager} compartido de Spring se cierra en {@code execute()} y el estandar JPA
     * del proyecto prohibe abrir un {@code EntityManager} desde la factory; el detalle sale de {@code usp_consultar_destinatarios_receso_academico}.
     */
    static final String SQL_CREAR = """
            EXEC dbo.usp_crear_receso_academico
                 @idPeriodo = :idPeriodo,
                 @fecha = :fecha,
                 @motivo = :motivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idReceso = :idReceso,
                 @incluirDetalle = 0
            """;
    static final String OP_RETIRAR = "retirarRecesoAcademico";
    static final String SQL_RETIRAR = """
            EXEC dbo.usp_retirar_receso_academico
                 @idReceso = :idReceso,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_recesos_periodo
                 @idPeriodo = :idPeriodo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_receso_academico
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idReceso = :idReceso
            """;

    /** Columnas extra de la tupla canonica de crear: idReceso, sesionesCanceladas, sesionesConAsistencia. */
    static final int COLUMNAS_EXTRA_CREAR = 3;
    /** Columnas de consultar: id, idPeriodoAcademico, fecha, motivo, sesionesCanceladas, estaAplicado, puedeRetirarse, fechaCreacionUtc, totalRegistros. */
    static final int COLUMNAS_CONSULTAR = 9;
    /** Columnas de destinatarios (contrato BD migracion 68). */
    static final int COLUMNAS_DESTINATARIOS = 12;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public RecesoAcademicoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del receso academico es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public RecesoCreado crear(final UUID idReceso, final UUID idPeriodo, final LocalDate fecha, final String motivo,
                              final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPeriodo", idPeriodo);
        parametros.put("fecha", fecha);
        parametros.put("motivo", motivo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        parametros.put("idReceso", idReceso);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_CREAR, SQL_CREAR, parametros, correlationId, COLUMNAS_EXTRA_CREAR);
        final List<Object> extras = fila.extras();
        return new RecesoCreado(NativeRowValues.uuid(extras.get(0)), NativeRowValues.entero(extras.get(1)),
                NativeRowValues.entero(extras.get(2)), fila.result().getMensajeUsuarioResultado());
    }

    @Override
    public String retirar(final UUID idReceso, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idReceso", idReceso);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        return procedureExecutor.execute(OP_RETIRAR, SQL_RETIRAR, parametros, correlationId).getMensajeUsuarioResultado();
    }

    @Override
    public List<RecesoPeriodo> consultar(final UUID idPeriodo, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarRecesosPeriodo",
                "No fue posible consultar los dias de receso del periodo.",
                () -> {
                    final List<?> filas = ejecutarLectura(SQL_CONSULTAR, "periodo", Map.of(
                            "idPeriodo", idPeriodo, "idCorrelacion", correlationId, "idUsuarioEjecutor", usuarioEjecutor));
                    final List<RecesoPeriodo> recesos = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = exigirColumnas((Object[]) fila, COLUMNAS_CONSULTAR, "usp_consultar_recesos_periodo");
                        recesos.add(new RecesoPeriodo(NativeRowValues.uuid(c[0]), NativeRowValues.uuid(c[1]), fechaDia(c[2]),
                                NativeRowValues.texto(c[3]), NativeRowValues.entero(c[4]), NativeRowValues.bit(c[5]),
                                NativeRowValues.bit(c[6]), SesionCancelacionJpaRepository.fechaHora(c[7])));
                    }
                    return recesos;
                });
    }

    @Override
    public List<DestinatarioReceso> consultarDestinatarios(final UUID idReceso, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarDestinatariosRecesoAcademico",
                "No fue posible consultar los destinatarios del aviso del receso academico.",
                () -> {
                    final List<?> filas = ejecutarLectura(SQL_DESTINATARIOS, "receso", Map.of(
                            "idReceso", idReceso, "idCorrelacion", correlationId, "idUsuarioEjecutor", usuarioEjecutor));
                    final List<DestinatarioReceso> destinatarios = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = exigirColumnas((Object[]) fila, COLUMNAS_DESTINATARIOS,
                                "usp_consultar_destinatarios_receso_academico");
                        destinatarios.add(new DestinatarioReceso(NativeRowValues.texto(c[0]), NativeRowValues.uuid(c[1]),
                                NativeRowValues.texto(c[2]), NativeRowValues.texto(c[3]), NativeRowValues.uuid(c[4]),
                                NativeRowValues.uuid(c[5]), NativeRowValues.texto(c[6]), NativeRowValues.texto(c[7]), fechaDia(c[8]),
                                SesionCancelacionJpaRepository.fechaHora(c[9]), SesionCancelacionJpaRepository.fechaHora(c[10]),
                                NativeRowValues.texto(c[11])));
                    }
                    return destinatarios;
                });
    }

    private List<?> ejecutarLectura(final String sql, final String entidadNoEncontrada, final Map<String, Object> parametros) {
        try {
            final var query = entityManager.createNativeQuery(sql);
            parametros.forEach(query::setParameter);
            return query.getResultList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception, entidadNoEncontrada);
        }
    }

    private static Object[] exigirColumnas(final Object[] fila, final int esperadas, final String procedimiento) {
        if (fila.length != esperadas) {
            throw new IllegalStateException(procedimiento + " debe devolver " + esperadas + " columnas y devolvio " + fila.length + ".");
        }
        return fila;
    }

    /** Columna DATE del SP: LocalDate, LocalDateTime o java.util.Date segun el driver; se recupera la fecha de pared. */
    static LocalDate fechaDia(final Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDate fecha -> fecha;
            case LocalDateTime fecha -> fecha.toLocalDate();
            case java.util.Date fecha -> LocalDate.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault());
            default -> LocalDate.parse(valor.toString().strip());
        };
    }
}
