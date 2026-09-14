package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Objects;

/**
 * Adaptador SQL Server para persistencia y consulta de sesiones de clase.
 */
public final class SesionRepositorySqlServerAdapter implements SesionRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesionRepositorySqlServerAdapter.class);

    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_ID_DOCENTE = "idDocente";
    private static final String PARAM_NOMBRE = "nombre";
    private static final String PARAM_DESCRIPCION = "descripcion";
    private static final String PARAM_FECHA_HORA_INICIO = "fechaHoraInicio";
    private static final String PARAM_FECHA_HORA_FIN = "fechaHoraFin";
    private static final String PARAM_AULA = "aula";
    private static final String PARAM_TIPO = "tipo";
    private static final String PARAM_ID_SESION = "idSesion";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";

    static final String SQL_CREAR_SESION = """
            EXEC dbo.usp_crear_sesion
                 @idGrupo = :idGrupo,
                 @idDocente = :idDocente,
                 @nombre = :nombre,
                 @descripcion = :descripcion,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @aula = :aula,
                 @tipo = :tipo,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_CERRAR_SESION = """
            EXEC dbo.usp_cerrar_sesion
                 @idSesion = :idSesion,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_ACTUALIZAR_SESION = """
            EXEC dbo.usp_actualizar_sesion
                 @idSesion = :idSesion,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @aula = :aula,
                 @descripcion = :descripcion,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_GENERAR_SESIONES_GRUPO = """
            EXEC dbo.usp_generar_sesiones_grupo
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_CONSULTAR_POR_ID = """
            SELECT TOP 1
                id,
                idGrupo,
                nombre,
                numero,
                codigo,
                numeroSemana,
                codigoGrupo,
                nombreGrupo,
                fechaHoraInicio,
                fechaHoraFin
            FROM dbo.uv_sesion
            WHERE id = :idSesion
            """;

    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations jdbcOperations;

    public SesionRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations
    ) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor canonico de sesiones es obligatorio.");
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "Las operaciones JDBC nombradas de sesiones son obligatorias.");
    }

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "crearSesion",
                SQL_CREAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_GRUPO, dto.getGrupo())
                        .addValue(PARAM_ID_DOCENTE, dto.getDocente())
                        .addValue(PARAM_NOMBRE, dto.getTema())
                        .addValue(PARAM_DESCRIPCION, dto.getDescripcion())
                        .addValue(PARAM_FECHA_HORA_INICIO, dto.getFechaHoraInicio())
                        .addValue(PARAM_FECHA_HORA_FIN, dto.getFechaHoraFin())
                        .addValue(PARAM_AULA, dto.getAula())
                        .addValue(PARAM_TIPO, dto.getTipo())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public void actualizarSesion(final ActualizarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para actualizar sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "actualizarSesion",
                SQL_ACTUALIZAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SESION, dto.sesion())
                        .addValue(PARAM_NOMBRE, dto.nombre())
                        .addValue(PARAM_FECHA_HORA_INICIO, dto.fechaHoraInicio())
                        .addValue(PARAM_FECHA_HORA_FIN, dto.fechaHoraFin())
                        .addValue(PARAM_AULA, dto.aula())
                        .addValue(PARAM_DESCRIPCION, dto.descripcion())
                        .addValue(PARAM_ID_DOCENTE, dto.docente())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        try {
            return jdbcOperations.query(SQL_CONSULTAR_POR_ID, new MapSqlParameterSource(PARAM_ID_SESION, dto.getSesion()), rs -> {
                if (!rs.next()) {
                    return null;
                }
                return new SesionRepositoryProjection(
                        JdbcValueMapper.toUuid(rs.getObject("id")),
                        JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                        JdbcValueMapper.toString(rs.getObject("nombre")),
                        JdbcValueMapper.toInteger(rs.getObject("numero")),
                        JdbcValueMapper.toString(rs.getObject("codigo")),
                        JdbcValueMapper.toInteger(rs.getObject("numeroSemana")),
                        JdbcValueMapper.toString(rs.getObject("codigoGrupo")),
                        JdbcValueMapper.toString(rs.getObject("nombreGrupo")),
                        JdbcValueMapper.toLocalDateTime(rs.getObject("fechaHoraInicio")),
                        JdbcValueMapper.toLocalDateTime(rs.getObject("fechaHoraFin"))
                );
            });
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarSesion, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar la sesion desde base de datos.", exception);
        }
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "cerrarSesion",
                SQL_CERRAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SESION, dto.getSesion())
                        .addValue(PARAM_ID_DOCENTE, dto.getDocente())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public void generarSesionesGrupo(final GenerarSesionesGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }

        procedureExecutor.execute(
                "generarSesionesGrupo",
                SQL_GENERAR_SESIONES_GRUPO,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_GRUPO, dto.grupo())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }
}
