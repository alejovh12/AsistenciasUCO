package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
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

/** Lectura de {@code uv_tipo_programa} y comandos sobre los SP de tipos de programa (HU023 / HU154). */
@Repository
public class TipoProgramaJpaRepository implements TipoProgramaQueryPort, TipoProgramaCommandPort {

    static final String SQL_CREAR = """
            EXEC dbo.usp_crear_tipo_programa
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CREAR = "crearTipoPrograma";

    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_tipo_programa
                 @id = :id,
                 @nombre = :nombre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_ACTUALIZAR = "actualizarTipoPrograma";

    static final String SQL_CAMBIAR_ESTADO = """
            EXEC dbo.usp_cambiar_estado_tipo_programa
                 @id = :id,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CAMBIAR_ESTADO = "cambiarEstadoTipoPrograma";

    static final String HQL_LISTAR = """
            select t
            from UvTipoProgramaEntity t
            order by t.nombre, t.id
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public TipoProgramaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de tipos de programa es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public void crearTipoPrograma(final CrearTipoProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("codigo", command.codigo());
        parametros.put("nombre", command.nombre());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_CREAR, SQL_CREAR, parametros, correlationId);
    }

    @Override
    public void actualizarTipoPrograma(final ActualizarTipoProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", command.id());
        parametros.put("nombre", command.nombre());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, correlationId);
    }

    @Override
    public void cambiarEstadoTipoPrograma(final CambiarEstadoTipoProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", command.id());
        parametros.put("estaActivo", command.activo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_CAMBIAR_ESTADO, SQL_CAMBIAR_ESTADO, parametros, correlationId);
    }

    @Override
    public List<TipoProgramaProjection> consultarTiposPrograma() {
        return JpaQueryExecutor.execute(
                "consultarTiposPrograma",
                "No fue posible consultar los tipos de programa desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvTipoProgramaEntity.class).getResultList().stream()
                        .map(row -> new TipoProgramaProjection(
                                row.id(), row.codigo(), row.nombre(), row.estaActivoTipoPrograma()))
                        .toList()
        );
    }
}
