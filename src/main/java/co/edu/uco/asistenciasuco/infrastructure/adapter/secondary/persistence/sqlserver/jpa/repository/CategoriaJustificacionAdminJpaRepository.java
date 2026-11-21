package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CategoriaJustificacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarCategoriaJustificacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
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
 * Administracion del catalogo de categorias de justificacion (migracion 65 de la BD): lectura de uv_categoria_justificacion_admin (activos e inactivos) y
 * escritura solo con los SP usp_crear/actualizar/cambiar_estado_categoria_justificacion, que exigen el perfil ADMINISTRADOR.
 */
@Repository
public class CategoriaJustificacionAdminJpaRepository implements CategoriaJustificacionAdminPort {

    static final String SQL_CONSULTAR = """
            SELECT a.id, a.codigo, a.nombre, a.descripcion, a.requiereSoporte, a.estaActivo FROM dbo.uv_categoria_justificacion_admin a ORDER BY a.nombre
            """;
    static final String SQL_CREAR = """
            EXEC dbo.usp_crear_categoria_justificacion
                 @id = :id, @codigo = :codigo, @nombre = :nombre, @descripcion = :descripcion, @requiereSoporte = :requiereSoporte,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_categoria_justificacion
                 @id = :id, @nombre = :nombre, @descripcion = :descripcion, @requiereSoporte = :requiereSoporte,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CAMBIAR_ESTADO = """
            EXEC dbo.usp_cambiar_estado_categoria_justificacion
                 @id = :id,
                 @estaActiva = :estaActiva,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CONSULTAR = "consultarCategoriaJustificacionAdmin";
    static final String OP_CREAR = "crearCategoriaJustificacion";
    static final String OP_ACTUALIZAR = "actualizarCategoriaJustificacion";
    static final String OP_CAMBIAR_ESTADO = "cambiarEstadoCategoriaJustificacion";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public CategoriaJustificacionAdminJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionAdminProjection> consultarTodos() {
        return JpaQueryExecutor.execute(OP_CONSULTAR, "No fue posible consultar el catalogo de categorias de justificacion.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CONSULTAR).getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new CategoriaJustificacionAdminProjection(NativeRowValues.uuid(f[0]), NativeRowValues.texto(f[1]), NativeRowValues.texto(f[2]),
                                NativeRowValues.texto(f[3]), NativeRowValues.bit(f[4]), NativeRowValues.bit(f[5])))
                        .toList());
    }

    @Override
    public ResultadoCatalogoAdminProjection crear(final GuardarCategoriaJustificacionCommand command) {
        final Map<String, Object> parametros = base(command.id(), command.usuario());
        parametros.put("nombre", command.nombre());
        parametros.put("codigo", command.codigo());
        parametros.put("descripcion", command.descripcion());
        parametros.put("requiereSoporte", command.requiereSoporte());
        return ejecutar(OP_CREAR, SQL_CREAR, parametros, command.id());
    }

    @Override
    public ResultadoCatalogoAdminProjection actualizar(final GuardarCategoriaJustificacionCommand command) {
        final Map<String, Object> parametros = base(command.id(), command.usuario());
        parametros.put("nombre", command.nombre());
        parametros.put("descripcion", command.descripcion());
        parametros.put("requiereSoporte", command.requiereSoporte());
        return ejecutar(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, command.id());
    }

    @Override
    public ResultadoCatalogoAdminProjection cambiarEstado(final UUID id, final boolean estaActiva, final UUID usuarioEjecutor) {
        final Map<String, Object> parametros = base(id, usuarioEjecutor);
        parametros.put("estaActiva", estaActiva);
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
