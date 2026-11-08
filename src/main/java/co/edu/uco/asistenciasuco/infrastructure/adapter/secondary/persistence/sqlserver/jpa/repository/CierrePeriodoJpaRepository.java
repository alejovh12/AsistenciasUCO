package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialCierrePeriodoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CierrePeriodoReporteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command de cierre masivo de periodo sobre JPA (LB-008 JPA-03) y Query de historial (F-ADM-006).
 */
@Repository
public class CierrePeriodoJpaRepository implements CierrePeriodoCommandPort, HistorialCierrePeriodoQueryPort {

    static final String SQL_EJECUTAR_CIERRE_MASIVO = """
            EXEC dbo.usp_ejecutar_cierre_masivo_periodo
                 @codigoPeriodo = :codigoPeriodo,
                 @idActor = :idActor,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR_HISTORIAL = """
            SELECT a.id,
                   ISNULL(p.nombre, 'Periodo') AS periodoCodigo,
                   a.occurredAt,
                   a.metadata,
                   a.result,
                   a.actorId
            FROM dbo.AuditoriaEvento a
            LEFT JOIN dbo.uv_periodo_academico p ON p.id = TRY_CAST(a.resourceId AS UNIQUEIDENTIFIER)
            WHERE a.action = 'CIERRE_MASIVO_PERIODO'
            ORDER BY a.occurredAt DESC
            """;

    static final String OP_EJECUTAR_CIERRE_MASIVO = "ejecutarCierreMasivoPeriodo";

    private final JpaProcedureExecutor procedureExecutor;
    private final EntityManager entityManager;

    public CierrePeriodoJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this(procedureExecutor, null);
    }

    public CierrePeriodoJpaRepository(final JpaProcedureExecutor procedureExecutor, final EntityManager entityManager) {
        this.procedureExecutor = Objects.requireNonNull(
                procedureExecutor,
                "El JpaProcedureExecutor del cierre de periodo es obligatorio."
        );
        this.entityManager = entityManager;
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

    @Override
    public List<CierrePeriodoReporteProjection> consultarHistorialCierres() {
        if (entityManager == null) {
            return List.of();
        }
        final List<?> resultados = entityManager.createNativeQuery(SQL_CONSULTAR_HISTORIAL).getResultList();
        final List<CierrePeriodoReporteProjection> lista = new ArrayList<>();
        for (final Object filaObj : resultados) {
            if (filaObj instanceof Object[] fila && fila.length >= 6) {
                final UUID id = NativeRowValues.uuid(fila[0]);
                final String periodoCodigo = NativeRowValues.texto(fila[1]);
                final OffsetDateTime fechaEjecucion = parseOffsetDateTime(fila[2]);
                final String metadata = NativeRowValues.texto(fila[3]);
                final String result = NativeRowValues.texto(fila[4]);
                final String actorId = NativeRowValues.texto(fila[5]);

                final int procesados = extraerEntero(metadata, "procesados");
                final int reprobados = extraerEntero(metadata, "reprobadosPorFallas");
                final int aprobados = Math.max(0, procesados - reprobados);
                final int materias = extraerEntero(metadata, "materiasAfectadas");
                final String estado = "SUCCESS".equalsIgnoreCase(result) ? "COMPLETADO" : "CON_ALERTAS";
                final String ejecutadoPor = actorId != null && !actorId.isBlank() ? actorId : "ADMIN_SISTEMA";

                lista.add(new CierrePeriodoReporteProjection(
                        id,
                        periodoCodigo != null ? periodoCodigo : "Periodo",
                        fechaEjecucion,
                        procesados,
                        materias,
                        aprobados,
                        reprobados,
                        estado,
                        ejecutadoPor
                ));
            }
        }
        return List.copyOf(lista);
    }

    private static OffsetDateTime parseOffsetDateTime(final Object valor) {
        if (valor == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (valor instanceof OffsetDateTime offset) {
            return offset;
        }
        if (valor instanceof java.time.Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (valor instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(ZoneOffset.UTC);
        }
        if (valor instanceof java.util.Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
        try {
            return OffsetDateTime.parse(valor.toString().strip().replace(' ', 'T'));
        } catch (final Exception _) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private static int extraerEntero(final String json, final String clave) {
        if (json == null) {
            return 0;
        }
        final Matcher matcher = Pattern.compile("\"" + clave + "\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
