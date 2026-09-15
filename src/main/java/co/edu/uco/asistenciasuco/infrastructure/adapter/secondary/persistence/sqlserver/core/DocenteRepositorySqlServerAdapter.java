package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.DocenteAsignacionAcademicaRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.DocenteIdentidadRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

/**
 * Adaptador SQL Server para operaciones confirmadas de docentes.
 */
public final class DocenteRepositorySqlServerAdapter implements DocenteRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocenteRepositorySqlServerAdapter.class);
    private static final RowMapper<DocenteIdentidadRepositoryProjection> DOCENTE_IDENTIDAD_ROW_MAPPER =
            new DocenteIdentidadRepositoryRowMapper();
    private static final RowMapper<DocenteAsignacionAcademicaRepositoryProjection> DOCENTE_ASIGNACION_ROW_MAPPER =
            new DocenteAsignacionAcademicaRepositoryRowMapper();
    private static final String OPERATION_CONSULTAR_DOCENTES = "consultarDocentes";
    private static final String OPERATION_CONSULTAR_DOCENTE_POR_ID = "consultarDocentePorId";
    private static final String OPERATION_CONSULTAR_ASIGNACIONES = "consultarAsignacionesAcademicas";

    static final String SQL_CONSULTAR_TODOS = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario
            FROM dbo.uv_docente_identidad
            ORDER BY nombreCompleto, id
            """;

    static final String SQL_CONSULTAR_POR_ID = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario
            FROM dbo.uv_docente_identidad
            WHERE id = ?
            """;
    static final String SQL_CONSULTAR_ASIGNACIONES = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario,
                idInstitucion,
                nombreInstitucion,
                idFacultad,
                nombreFacultad,
                idPrograma,
                nombrePrograma,
                idPlanEstudio,
                inpPlanEstudio,
                idAsignatura,
                nombreAsignatura,
                idGrupo,
                nombreGrupo,
                idPerfil,
                codigoPerfil,
                nombrePerfil,
                estaActivoDocente,
                estaActivoTextoDocente
            FROM dbo.uv_docente
            WHERE id = ?
            ORDER BY
                nombreInstitucion,
                nombreFacultad,
                nombrePrograma,
                nombreAsignatura,
                nombreGrupo,
                idGrupo
            """;

    private final QueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentesExecutor;
    private final ParameterizedQueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentePorIdExecutor;
    private final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesExecutor;

    public DocenteRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para docentes es obligatorio.");

        this.consultarDocentesExecutor = sql -> jdbcTemplate.query(sql, DOCENTE_IDENTIDAD_ROW_MAPPER);
        this.consultarDocentePorIdExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DOCENTE_IDENTIDAD_ROW_MAPPER, args);
        this.consultarAsignacionesExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DOCENTE_ASIGNACION_ROW_MAPPER, args);
    }

    DocenteRepositorySqlServerAdapter(
            final QueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentesExecutor,
            final ParameterizedQueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentePorIdExecutor,
            final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesExecutor
    ) {
        this.consultarDocentesExecutor = Objects.requireNonNull(
                consultarDocentesExecutor,
                "El ejecutor para consultar docentes es obligatorio."
        );
        this.consultarDocentePorIdExecutor = Objects.requireNonNull(
                consultarDocentePorIdExecutor,
                "El ejecutor para consultar docente por ID es obligatorio."
        );
        this.consultarAsignacionesExecutor = Objects.requireNonNull(
                consultarAsignacionesExecutor,
                "El ejecutor para consultar asignaciones de docente es obligatorio."
        );
    }

    @Override
    public List<DocenteIdentidadRepositoryProjection> consultarDocentes() {
        try {
            return consultarDocentesExecutor.query(SQL_CONSULTAR_TODOS);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_DOCENTES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los docentes.", exception);
        }
    }

    @Override
    public Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorId(
            final ConsultarDocentePorIdRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar docente por ID es obligatorio.");
        }

        try {
            final List<DocenteIdentidadRepositoryProjection> resultado =
                    consultarDocentePorIdExecutor.query(SQL_CONSULTAR_POR_ID, dto.getDocente().toString());
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_DOCENTE_POR_ID,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar el docente por ID.", exception);
        }
    }

    @Override
    public List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesAcademicas(
            final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar asignaciones academicas del docente es obligatorio.");
        }

        try {
            return consultarAsignacionesExecutor.query(SQL_CONSULTAR_ASIGNACIONES, dto.getDocente().toString());
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_ASIGNACIONES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    "No fue posible consultar las asignaciones academicas del docente.",
                    exception
            );
        }
    }

    @Override
    public DocenteOperacionRepositoryProjection registrarDocenteDesdeUsuario(
            final RegistrarDocenteDesdeUsuarioRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar docente desde usuario es obligatorio.");
        }
        throw new FeatureUnavailableException(
                "El registro de docente desde usuario requiere un command publico de DB; los procedimientos internos no estan permitidos."
        );
    }

    @Override
    public DocenteOperacionRepositoryProjection asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para asignar docente a grupo es obligatorio.");
        }
        throw new FeatureUnavailableException(
                "La asignacion de docente a grupo requiere un command publico de DB; los procedimientos internos no estan permitidos."
        );
    }

    interface QueryExecutor<T> {

        List<T> query(String sql);
    }

    interface ParameterizedQueryExecutor<T> {

        List<T> query(String sql, Object... args);
    }

}
