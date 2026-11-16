package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * INT-003: {@code usp_sincronizar_mensaje_usuario} (JPA, {@code EXEC} nativo). La BD actualiza solo el texto de un
 * codigo que ya existe; devuelve la tupla canonica + {@code codigo}, {@code huboCambio}, {@code fechaModificacion} y
 * {@code existeMensaje}.
 *
 * <p>Un codigo que la BD no tiene (por ejemplo, los textos de interfaz {@code UI_*} que solo viven en MessageUcoLab) no es
 * un error: el SP responde {@code estadoResultado = 0} con {@code existeMensaje = 0} y aqui se informa
 * {@link ResultadoSincronizacion#NO_EXISTE}. Cualquier otro rechazo se traduce con el validador canonico.</p>
 */
@Repository
public class MensajeCatalogoJpaRepository implements MensajeCatalogoRepositoryPort {

    static final String OP_SINCRONIZAR = "sincronizarMensajeUsuario";
    static final String SQL_SINCRONIZAR = """
            EXEC dbo.usp_sincronizar_mensaje_usuario
                 @codigo = :codigo, @contenido = :contenido, @idCorrelacion = :idCorrelacion
            """;
    /** codigo, huboCambio, fechaModificacion, existeMensaje. */
    static final int COLUMNAS_EXTRA = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(MensajeCatalogoJpaRepository.class);

    private final EntityManager entityManager;

    public MensajeCatalogoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de mensajes es obligatorio.");
    }

    @Override
    public ResultadoSincronizacion sincronizarMensajeUsuario(final String codigo, final String contenido) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("codigo", codigo);
        parametros.put("contenido", contenido);
        parametros.put("idCorrelacion", correlationId);
        final ProcedureRow fila = ejecutar(parametros);
        final List<Object> extras = fila.extras();
        final Object existe = extras.get(3);
        if (!fila.result().getEstadoResultado() && existe != null && !NativeRowValues.bit(existe)) {
            return ResultadoSincronizacion.NO_EXISTE;
        }
        ProcedureResultValidator.validate(fila.result(), correlationId, OP_SINCRONIZAR);
        return NativeRowValues.bit(extras.get(1)) ? ResultadoSincronizacion.ACTUALIZADO : ResultadoSincronizacion.SIN_CAMBIOS;
    }

    private ProcedureRow ejecutar(final Map<String, Object> parametros) {
        try {
            final Query consulta = entityManager.createNativeQuery(SQL_SINCRONIZAR);
            parametros.forEach(consulta::setParameter);
            return ProcedureResultMapper.mapSingleWithExtras(consulta.getResultList(), COLUMNAS_EXTRA);
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error("SQL operation failed. operation={}, correlationId={}", OP_SINCRONIZAR, CorrelationIdContext.getAsString());
            throw new DatabaseOperationException(DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible ejecutar el procedimiento almacenado.", exception);
        }
    }
}
