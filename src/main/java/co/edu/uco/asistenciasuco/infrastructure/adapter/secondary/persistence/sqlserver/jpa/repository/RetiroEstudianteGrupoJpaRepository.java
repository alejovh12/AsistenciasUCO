package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU051: retiro de un estudiante del grupo ({@code usp_retirar_estudiante_grupo}, resultset canonico) y destinatarios
 * del aviso ({@code usp_consultar_destinatarios_retiro_estudiante_grupo}). La titularidad la valida la DB.
 */
@Repository
public class RetiroEstudianteGrupoJpaRepository implements RetiroEstudianteGrupoPort {

    static final String OP_RETIRAR = "retirarEstudianteGrupo";
    static final String OP_DESTINATARIOS = "consultarDestinatariosRetiroEstudianteGrupo";

    static final String SQL_RETIRAR = """
            EXEC dbo.usp_retirar_estudiante_grupo
                 @idGrupo = :idGrupo,
                 @idEstudiante = :idEstudiante,
                 @motivo = :motivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_retiro_estudiante_grupo
                 @idGrupo = :idGrupo,
                 @idEstudiante = :idEstudiante,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private final JpaProcedureExecutor procedureExecutor;
    private final EntityManager entityManager;

    public RetiroEstudianteGrupoJpaRepository(
            final JpaProcedureExecutor procedureExecutor,
            final EntityManager entityManager
    ) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public void retirarEstudiante(
            final UUID idGrupo,
            final UUID idEstudiante,
            final String motivo,
            final UUID idUsuarioEjecutor
    ) {
        if (ObjectHelper.isNull(idUsuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor del retiro es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", idGrupo);
        parametros.put("idEstudiante", idEstudiante);
        parametros.put("motivo", motivo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);
        procedureExecutor.execute(OP_RETIRAR, SQL_RETIRAR, parametros, correlationId);
    }

    @Override
    public List<DestinatarioRetiroProjection> consultarDestinatarios(
            final UUID idGrupo,
            final UUID idEstudiante,
            final UUID idUsuarioEjecutor
    ) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_DESTINATARIOS,
                "No fue posible consultar los destinatarios del aviso de retiro desde base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_DESTINATARIOS)
                        .setParameter("idGrupo", idGrupo)
                        .setParameter("idEstudiante", idEstudiante)
                        .setParameter("idCorrelacion", correlationId)
                        .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                        .getResultList()).stream()
                        .map(fila -> toProjection((Object[]) fila))
                        .toList()
        );
    }

    private static DestinatarioRetiroProjection toProjection(final Object[] fila) {
        return new DestinatarioRetiroProjection(
                (String) fila[0],
                (UUID) fila[1],
                (String) fila[2],
                (String) fila[3],
                (UUID) fila[4],
                fila[5] == null ? null : ((Number) fila[5]).intValue(),
                (String) fila[6],
                (String) fila[7]
        );
    }
}
