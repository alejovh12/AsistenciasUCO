package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU096/HU097/HU098: comandos EXEC de periodos academicos (la consulta vive en {@link PeriodoAcademicoJpaRepository}). */
@Repository
public class PeriodoAcademicoCommandJpaRepository implements PeriodoAcademicoCommandPort {

    static final String SQL_CREAR_PERIODO = """
            EXEC dbo.usp_crear_periodo_academico
                 @idPeriodo = :idPeriodo,
                 @nombre = :nombre,
                 @fechaInicio = :fechaInicio,
                 @fechaFin = :fechaFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_PERIODO = """
            EXEC dbo.usp_actualizar_periodo_academico
                 @idPeriodo = :idPeriodo,
                 @nombre = :nombre,
                 @fechaInicio = :fechaInicio,
                 @fechaFin = :fechaFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ELIMINAR_PERIODO = """
            EXEC dbo.usp_eliminar_periodo_academico
                 @idPeriodo = :idPeriodo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_CREAR_PERIODO = "crearPeriodoAcademico";
    static final String OP_ACTUALIZAR_PERIODO = "actualizarPeriodoAcademico";
    static final String OP_ELIMINAR_PERIODO = "eliminarPeriodoAcademico";

    private final JpaProcedureExecutor procedureExecutor;

    public PeriodoAcademicoCommandJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public void crearPeriodoAcademico(final UUID idPeriodo, final String nombre, final LocalDate fechaInicio,
                                      final LocalDate fechaFin, final UUID idUsuarioEjecutor) {
        ejecutar(OP_CREAR_PERIODO, SQL_CREAR_PERIODO, datosPeriodo(idPeriodo, nombre, fechaInicio, fechaFin), idUsuarioEjecutor);
    }

    @Override
    public void actualizarPeriodoAcademico(final UUID idPeriodo, final String nombre, final LocalDate fechaInicio,
                                           final LocalDate fechaFin, final UUID idUsuarioEjecutor) {
        ejecutar(OP_ACTUALIZAR_PERIODO, SQL_ACTUALIZAR_PERIODO, datosPeriodo(idPeriodo, nombre, fechaInicio, fechaFin),
                idUsuarioEjecutor);
    }

    @Override
    public void eliminarPeriodoAcademico(final UUID idPeriodo, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPeriodo", idPeriodo);
        ejecutar(OP_ELIMINAR_PERIODO, SQL_ELIMINAR_PERIODO, parametros, idUsuarioEjecutor);
    }

    private static Map<String, Object> datosPeriodo(final UUID idPeriodo, final String nombre,
                                                    final LocalDate fechaInicio, final LocalDate fechaFin) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPeriodo", idPeriodo);
        parametros.put("nombre", nombre);
        parametros.put("fechaInicio", fechaInicio);
        parametros.put("fechaFin", fechaFin);
        return parametros;
    }

    private void ejecutar(final String operacion, final String sql, final Map<String, Object> parametros,
                          final UUID idUsuarioEjecutor) {
        if (ObjectHelper.isNull(idUsuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor del periodo academico es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);
        procedureExecutor.execute(operacion, sql, parametros, correlationId);
    }
}
