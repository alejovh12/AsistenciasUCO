package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Command de cierre masivo de periodo sobre JPA (LB-008 JPA-03). {@code usp_ejecutar_cierre_masivo_periodo} administra
 * su propia transaccion ({@code BEGIN TRANSACTION} ... {@code COMMIT}, {@code ROLLBACK} en CATCH) y se invoca SIN
 * transaccion exterior (CURRENT_OUTER_TX = NO), igual que hoy: TARGET_OUTER_TX = NO.
 */
@Repository
public class CierrePeriodoJpaRepository implements CierrePeriodoCommandPort {

    static final String SQL_EJECUTAR_CIERRE_MASIVO = """
            EXEC dbo.usp_ejecutar_cierre_masivo_periodo
                 @codigoPeriodo = :codigoPeriodo,
                 @idActor = :idActor,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_EJECUTAR_CIERRE_MASIVO = "ejecutarCierreMasivoPeriodo";

    private final JpaProcedureExecutor procedureExecutor;

    public CierrePeriodoJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(
                procedureExecutor,
                "El JpaProcedureExecutor del cierre de periodo es obligatorio."
        );
    }

    private ProcedureResult ejecutarCierreMasivoPeriodoConResultado(
            final String codigoPeriodo,
            final String idActor,
            final UUID idUsuarioEjecutor
    ) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("codigoPeriodo", codigoPeriodo);
        parametros.put("idActor", idActor);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);

        return procedureExecutor.execute(OP_EJECUTAR_CIERRE_MASIVO, SQL_EJECUTAR_CIERRE_MASIVO, parametros, correlationId);
    }

    @Override
    public void ejecutarCierreMasivoPeriodo(final String codigoPeriodo, final String idActor,
                                            final UUID idUsuarioEjecutor) {
        ejecutarCierreMasivoPeriodoConResultado(codigoPeriodo, idActor, idUsuarioEjecutor);
    }
}
