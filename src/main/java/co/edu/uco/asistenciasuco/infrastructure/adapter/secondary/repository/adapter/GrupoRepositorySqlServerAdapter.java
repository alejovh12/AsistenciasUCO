package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.GrupoRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

/**
 * Adaptador SQL Server para operaciones confirmadas de grupos.
 */
public final class GrupoRepositorySqlServerAdapter implements GrupoRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoRepositorySqlServerAdapter.class);
    private static final RowMapper<GrupoRepositoryProjection> GRUPO_ROW_MAPPER = new GrupoRepositoryRowMapper();
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";
    private static final String OPERATION_CONSULTAR_GRUPOS = "consultarGrupos";

    static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";
    static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    static final String PARAM_CORREO = "correo";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_ID_GRUPO = "idGrupo";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String OUT_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    static final String OUT_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    static final String OUT_ESTADO = "estadoResultado";

    static final String SQL_REGISTRAR_ESTUDIANTE = """
            EXEC dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente
                @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                @numeroIdentificacion = :numeroIdentificacion,
                @primerApellido = :primerApellido,
                @segundoApellido = :segundoApellido,
                @primerNombre = :primerNombre,
                @segundoNombre = :segundoNombre,
                @correo = :correo,
                @password = :password,
                @idGrupo = :idGrupo,
                @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_CONSULTAR_GRUPOS = """
            SELECT
                id,
                codigo,
                nombre,
                idAsignatura,
                nombreAsignatura,
                idDocente,
                capacidadMaximaPermitida,
                estudiantesActivos,
                cuposDisponibles,
                grupoEstaHablitado,
                fechaInicioPeriodoAcademico,
                fechaFinPeriodoAcademico
            FROM dbo.uv_grupo
            ORDER BY nombreAsignatura, codigo, nombre, id
            """;

    private final ProcedureQueryExecutor procedureQueryExecutor;
    private final GrupoQueryExecutor grupoQueryExecutor;
    private final TransactionOperations transactionOperations;

    public GrupoRepositorySqlServerAdapter(
            final JdbcTemplate jdbcTemplate,
            final TransactionTemplate transactionTemplate
    ) {
        Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para grupos es obligatorio.");
        Objects.requireNonNull(transactionTemplate, "El TransactionTemplate para grupos es obligatorio.");

        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.procedureQueryExecutor = namedParameterJdbcTemplate::queryForMap;
        this.grupoQueryExecutor = (sql, rowMapper) -> jdbcTemplate.query(sql, rowMapper);
        this.transactionOperations = transactionTemplate;
    }

    GrupoRepositorySqlServerAdapter(
            final ProcedureQueryExecutor procedureQueryExecutor,
            final GrupoQueryExecutor grupoQueryExecutor,
            final TransactionOperations transactionOperations
    ) {
        this.procedureQueryExecutor = Objects.requireNonNull(
                procedureQueryExecutor,
                "El ejecutor del procedimiento de grupos es obligatorio."
        );
        this.grupoQueryExecutor = Objects.requireNonNull(
                grupoQueryExecutor,
                "El ejecutor de consulta de grupos es obligatorio."
        );
        this.transactionOperations = Objects.requireNonNull(
                transactionOperations,
                "El manejador transaccional de grupos es obligatorio."
        );
    }

    @Override
    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }

        try {
            return transactionOperations.execute(status -> ejecutarRegistro(dto));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_REGISTRAR_ESTUDIANTE,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de registro de estudiante en grupo.",
                    exception
            );
        }
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos() {
        try {
            return grupoQueryExecutor.query(SQL_CONSULTAR_GRUPOS, GRUPO_ROW_MAPPER);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_GRUPOS,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los grupos.", exception);
        }
    }

    private RegistrarEstudianteRepositoryProjection ejecutarRegistro(final RegistrarEstudianteRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> result = procedureQueryExecutor.queryForMap(
                SQL_REGISTRAR_ESTUDIANTE,
                buildParameters(dto, correlationId)
        );

        DbExceptionTranslator.throwIfFailed(
                JdbcValueMapper.toBoolean(result.get(OUT_ESTADO)),
                JdbcValueMapper.toString(result.get(OUT_MENSAJE_USUARIO)),
                JdbcValueMapper.toString(result.get(OUT_MENSAJE_TECNICO)),
                correlationId.toString(),
                OPERATION_REGISTRAR_ESTUDIANTE
        );

        return new RegistrarEstudianteRepositoryProjection(JdbcValueMapper.toString(result.get(OUT_MENSAJE_USUARIO)));
    }

    private MapSqlParameterSource buildParameters(final RegistrarEstudianteRepositoryDTO dto, final UUID correlationId) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdentificacionId().toString())
                .addValue(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion())
                .addValue(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido())
                .addValue(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido())
                .addValue(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre())
                .addValue(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre())
                .addValue(PARAM_CORREO, dto.getCorreo())
                .addValue(PARAM_PASSWORD, dto.getPassword())
                .addValue(PARAM_ID_GRUPO, dto.getGrupoId().toString())
                .addValue(PARAM_ID_CORRELACION, correlationId.toString());
    }

    interface ProcedureQueryExecutor {

        Map<String, Object> queryForMap(String sql, MapSqlParameterSource parameters);
    }

    interface GrupoQueryExecutor {

        List<GrupoRepositoryProjection> query(String sql, RowMapper<GrupoRepositoryProjection> rowMapper);
    }
}
