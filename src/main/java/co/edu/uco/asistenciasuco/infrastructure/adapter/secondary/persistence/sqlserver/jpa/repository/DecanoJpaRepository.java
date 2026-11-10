package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.CrearDecanoCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Repositorio JPA directo consolidado para Decano. */
@Repository
public class DecanoJpaRepository implements DecanoQueryPort, DecanoCommandPort {

    static final String SQL_CREAR_DECANO = """
            EXEC dbo.usp_crear_decano
                 @idDecano = :idDecano,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idFacultad = :idFacultad,
                 @nombreFacultad = :nombreFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    // DEC-065 (5): una sola transaccion de BD para persona + rol + facultad. Resultset canonico + idDecano, idFacultad, nombreFacultad.
    static final String SQL_CREAR_DECANO_CON_FACULTAD = """
            EXEC dbo.usp_crear_decano_con_facultad
                 @idDecano = :idDecano,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @nombreFacultad = :nombreFacultad,
                 @codigoFacultad = :codigoFacultad,
                 @idInstitucion = :idInstitucion,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    static final String HQL_LISTAR = """
            select d.id, d.idUsuario, d.numeroIdentificacion, d.nombreCompleto,
                   d.idFacultad, d.nombreFacultad, d.estaActivoDecano
            from UvDecanoEntity d
            order by d.nombreCompleto, d.id
            """;

    static final String OP_CREAR_DECANO = "crearDecano";
    static final String OP_CREAR_DECANO_CON_FACULTAD = "crearDecanoConFacultad";
    static final String OP_CAMBIAR_ESTADO_DECANO = "cambiarEstadoDecano";

    // CULM-001 A.2-8: resultset canonico mas idUsuario, personaSinRolesActivos y estaActivoDecano.
    static final String SQL_CAMBIAR_ESTADO_DECANO = """
            EXEC dbo.usp_cambiar_estado_decano
                 @idDecano = :idDecano,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public DecanoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearDecano(final CrearDecanoCommand command) {
        ejecutarCrearDecano(command);
    }

    @Override
    public ResultadoDecanoConFacultad crearDecanoConFacultad(final CrearDecanoConFacultadCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDecano", command.idDecano());
        parametros.put("numeroIdentificacion", command.numeroIdentificacion());
        parametros.put("primerNombre", command.primerNombre());
        parametros.put("segundoNombre", command.segundoNombre());
        parametros.put("primerApellido", command.primerApellido());
        parametros.put("segundoApellido", command.segundoApellido());
        parametros.put("correo", command.correo());
        parametros.put("password", command.password());
        parametros.put("nombreFacultad", command.nombreFacultad());
        parametros.put("codigoFacultad", command.codigoFacultad());
        parametros.put("idInstitucion", command.idInstitucion());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        parametros.put("idTipoIdIdentificacion", command.tipoIdentificacionId());

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CREAR_DECANO_CON_FACULTAD, SQL_CREAR_DECANO_CON_FACULTAD, parametros, correlationId, 3);
        return new ResultadoDecanoConFacultad(
                idObligatorio(fila.extras().get(0), "idDecano"),
                idObligatorio(fila.extras().get(1), "idFacultad"),
                NativeRowValues.texto(fila.extras().get(2)));
    }

    private static UUID idObligatorio(final Object valor, final String columna) {
        try {
            final UUID id = NativeRowValues.uuid(valor);
            if (id != null) {
                return id;
            }
        } catch (final IllegalArgumentException exception) {
            throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento " + OP_CREAR_DECANO_CON_FACULTAD + " retorno " + columna + " con formato invalido.", exception);
        }
        throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El procedimiento " + OP_CREAR_DECANO_CON_FACULTAD + " no retorno " + columna + ".");
    }

    @Override
    public ResultadoCambioEstadoDecano cambiarEstadoDecano(final UUID idDecano, final Boolean estaActivo, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDecano", idDecano);
        // Un Boolean nulo sin tipo no tiene JdbcType en Hibernate: viaja como texto o NULL (NULL = invertir el estado).
        parametros.put("estaActivo", estaActivo == null ? null : (Boolean.TRUE.equals(estaActivo) ? "1" : "0"));
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CAMBIAR_ESTADO_DECANO, SQL_CAMBIAR_ESTADO_DECANO, parametros, correlationId, 3);
        return new ResultadoCambioEstadoDecano(idUsuario(fila.extras().get(0)), flag(fila.extras().get(1)), flag(fila.extras().get(2)));
    }

    private static UUID idUsuario(final Object valor) {
        try {
            final UUID id = NativeRowValues.uuid(valor);
            if (id != null) {
                return id;
            }
        } catch (final IllegalArgumentException exception) {
            throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento " + OP_CAMBIAR_ESTADO_DECANO + " retorno un usuario con formato invalido.", exception);
        }
        throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El procedimiento " + OP_CAMBIAR_ESTADO_DECANO + " no retorno el usuario del decano.");
    }

    private static boolean flag(final Object valor) {
        if (valor instanceof Boolean bit) {
            return bit;
        }
        return valor instanceof Number numero && numero.intValue() == 1;
    }

    // QUERIES

    @Override
    public List<DecanoProjection> consultarDecanos() {
        return JpaQueryExecutor.execute(
                "consultarDecanos",
                "No fue posible consultar los decanos desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, Object[].class).getResultList().stream()
                        .map(row -> new DecanoProjection(
                                (UUID) row[0],
                                (UUID) row[1],
                                row[2] == null ? null : String.valueOf(row[2]),
                                (String) row[3],
                                (UUID) row[4],
                                (String) row[5],
                                row[6] != null && ((Integer) row[6]) == 1
                        ))
                        .toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarCrearDecano(final CrearDecanoCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDecano", command.idDecano());
        parametros.put("numeroIdentificacion", command.numeroIdentificacion());
        parametros.put("primerNombre", command.primerNombre());
        parametros.put("segundoNombre", command.segundoNombre());
        parametros.put("primerApellido", command.primerApellido());
        parametros.put("segundoApellido", command.segundoApellido());
        parametros.put("correo", command.correo());
        parametros.put("idFacultad", command.idFacultad());
        parametros.put("nombreFacultad", command.nombreFacultad());
        parametros.put("password", command.password());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        // CULM-001 D.9-a (F-ADM-011): el tipo de documento elegido por el administrador (NULL = el que decide la BD).
        parametros.put("idTipoIdIdentificacion", command.tipoIdentificacionId());

        return procedureExecutor.execute(OP_CREAR_DECANO, SQL_CREAR_DECANO, parametros, correlationId);
    }
}
