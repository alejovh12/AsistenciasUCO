package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.MotivoCancelacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarMotivoCancelacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Administracion del catalogo de motivos de cancelacion de sesion (migracion 65 de la BD): lectura de uv_motivo_cancelacion_sesion (activos e inactivos) y
 * escritura solo con los SP usp_crear/actualizar/cambiar_estado_motivo_cancelacion_sesion, que exigen el perfil ADMINISTRADOR.
 */
@Repository
public class MotivoCancelacionAdminJpaRepository implements MotivoCancelacionAdminPort {

    static final String SQL_CONSULTAR = """
            SELECT a.id, a.codigo, a.nombre, a.orden, a.estaActivo FROM dbo.uv_motivo_cancelacion_sesion a ORDER BY a.orden, a.nombre
            """;
    static final String SQL_CREAR = """
            EXEC dbo.usp_crear_motivo_cancelacion_sesion
                 @id = :id, @codigo = :codigo, @nombre = :nombre, @orden = :orden,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_motivo_cancelacion_sesion
                 @id = :id, @nombre = :nombre, @orden = :orden,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CAMBIAR_ESTADO = """
            EXEC dbo.usp_cambiar_estado_motivo_cancelacion_sesion
                 @id = :id,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CONSULTAR = "consultarMotivoCancelacionAdmin";
    static final String OP_CREAR = "crearMotivoCancelacion";
    static final String OP_ACTUALIZAR = "actualizarMotivoCancelacion";
    static final String OP_CAMBIAR_ESTADO = "cambiarEstadoMotivoCancelacion";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public MotivoCancelacionAdminJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public List<MotivoCancelacionAdminProjection> consultarTodos() {
        return JpaQueryExecutor.execute(OP_CONSULTAR, "No fue posible consultar el catalogo de motivos de cancelacion de sesion.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CONSULTAR).getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new MotivoCancelacionAdminProjection(NativeRowValues.uuid(f[0]), NativeRowValues.texto(f[1]), NativeRowValues.texto(f[2]),
                                NativeRowValues.entero(f[3]), NativeRowValues.bit(f[4])))
                        .toList());
    }

    @Override
    public ResultadoCatalogoAdminProjection crear(final GuardarMotivoCancelacionCommand command) {
        final Map<String, Object> parametros = base(command.id(), command.usuario());
        parametros.put("nombre", command.nombre());
        parametros.put("codigo", command.codigo());
        parametros.put("orden", command.orden());
        return ejecutar(OP_CREAR, SQL_CREAR, parametros, command.id());
    }

    @Override
    public ResultadoCatalogoAdminProjection actualizar(final GuardarMotivoCancelacionCommand command) {
        final Map<String, Object> parametros = base(command.id(), command.usuario());
        parametros.put("nombre", command.nombre());
        parametros.put("orden", command.orden());
        return ejecutar(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, command.id());
    }

    @Override
    public ResultadoCatalogoAdminProjection cambiarEstado(final UUID id, final boolean estaActivo, final UUID usuarioEjecutor) {
        final Map<String, Object> parametros = base(id, usuarioEjecutor);
        parametros.put("estaActivo", estaActivo);
        return ejecutar(OP_CAMBIAR_ESTADO, SQL_CAMBIAR_ESTADO, parametros, id);
    }

    private static Map<String, Object> base(final UUID id, final UUID usuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", id);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        return parametros;
    }

    private ResultadoCatalogoAdminProjection ejecutar(final String operacion, final String sql,
                                                      final Map<String, Object> parametros, final UUID id) {
        final UUID correlationId = CorrelationIdContext.require();
        parametros.put("idCorrelacion", correlationId);
        final String mensaje = procedureExecutor.execute(operacion, sql, parametros, correlationId).getMensajeUsuarioResultado();
        return new ResultadoCatalogoAdminProjection(id, mensaje);
    }
}
