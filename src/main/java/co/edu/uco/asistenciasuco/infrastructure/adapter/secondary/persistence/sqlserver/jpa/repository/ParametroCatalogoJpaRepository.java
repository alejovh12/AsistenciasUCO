package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * INT-004: {@code usp_sincronizar_parametro_catalogo_externo} (JPA, {@code EXEC} nativo). La BD copia el valor de un parametro que
 * ya existe; devuelve la tupla canonica + {@code resultado}, {@code grupo}, {@code clave}, {@code huboCambio} y
 * {@code fechaModificacion}.
 *
 * <p>Un valor que no cumple el tipo, rango o lista del parametro no es un fallo tecnico: el SP responde
 * {@code estadoResultado = 0} con {@code resultado = RECHAZADO} y aqui se informa
 * {@link ResultadoSincronizacion#RECHAZADO} (reintentarlo no lo arregla). Cualquier otro rechazo (correlacion ausente, error SQL) se
 * traduce con el validador canonico y se propaga para que el evento se reintente.</p>
 */
@Repository
public class ParametroCatalogoJpaRepository implements ParametroCatalogoRepositoryPort {

    static final String OP_SINCRONIZAR = "sincronizarParametroCatalogoExterno";
    static final String SQL_SINCRONIZAR = """
            EXEC dbo.usp_sincronizar_parametro_catalogo_externo
                 @grupo = :grupo, @clave = :clave, @valor = :valor, @operacion = :operacion,
                 @idEvento = :idEvento, @ocurridoEnUtc = :ocurridoEnUtc, @idCorrelacion = :idCorrelacion
            """;
    /** resultado, grupo, clave, huboCambio, fechaModificacion. */
    static final int COLUMNAS_EXTRA = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametroCatalogoJpaRepository.class);

    private final EntityManager entityManager;

    public ParametroCatalogoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de parametros es obligatorio.");
    }

    @Override
    public ResultadoSincronizacion sincronizarParametroExterno(final String grupo, final String clave, final String valor,
                                                               final Operacion operacion, final String idEvento,
                                                               final Instant ocurridoEn) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("grupo", grupo);
        parametros.put("clave", clave);
        parametros.put("valor", valor);
        parametros.put("operacion", operacion.name());
        parametros.put("idEvento", idEvento);
        parametros.put("ocurridoEnUtc", LocalDateTime.ofInstant(ocurridoEn, ZoneOffset.UTC));
        parametros.put("idCorrelacion", correlationId);
        final ProcedureRow fila = ejecutar(parametros);
        final Object resultado = fila.extras().get(0);
        final ResultadoSincronizacion rechazado = resultado == null ? null : tryResultado(resultado.toString());
        if (!fila.result().getEstadoResultado() && rechazado == ResultadoSincronizacion.RECHAZADO) {
            return rechazado;
        }
        ProcedureResultValidator.validate(fila.result(), correlationId, OP_SINCRONIZAR);
        if (rechazado == null) {
            throw new DatabaseOperationException(DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "El procedimiento de sincronizacion de parametros devolvio un resultado desconocido.");
        }
        return rechazado;
    }

    private static ResultadoSincronizacion tryResultado(final String texto) {
        try {
            return ResultadoSincronizacion.valueOf(texto.trim());
        } catch (final IllegalArgumentException e) {
            return null;
        }
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
