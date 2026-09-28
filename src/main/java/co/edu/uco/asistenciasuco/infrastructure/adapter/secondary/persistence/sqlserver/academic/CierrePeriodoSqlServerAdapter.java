package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Objects;
import java.util.UUID;

public final class CierrePeriodoSqlServerAdapter implements CierrePeriodoCommandPort {

    private static final String ID_CORRELACION = "idCorrelacion";
    private final CanonicalStoredProcedureExecutor procedureExecutor;

    public CierrePeriodoSqlServerAdapter(final CanonicalStoredProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "CanonicalStoredProcedureExecutor es obligatorio.");
    }

    @Override
    public void ejecutarCierreMasivoPeriodo(final String codigoPeriodo, final String idActor, final UUID idUsuarioEjecutor) {
        procedureExecutor.execute("ejecutarCierreMasivoPeriodo", """
                EXEC dbo.usp_ejecutar_cierre_masivo_periodo
                     @codigoPeriodo = :codigoPeriodo,
                     @idActor = :idActor,
                     @idCorrelacion = :idCorrelacion,
                     @idUsuarioEjecutor = :idUsuarioEjecutor
                """, new MapSqlParameterSource()
                .addValue("codigoPeriodo", codigoPeriodo)
                .addValue("idActor", idActor)
                .addValue(ID_CORRELACION, CorrelationIdContext.require())
                .addValue("idUsuarioEjecutor", idUsuarioEjecutor));
    }
}
