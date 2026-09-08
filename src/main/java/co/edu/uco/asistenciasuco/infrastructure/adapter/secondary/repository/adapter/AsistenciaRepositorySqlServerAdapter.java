package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server para persistencia y consulta de asistencias y solicitudes de revision.
 */
public final class AsistenciaRepositorySqlServerAdapter implements AsistenciaRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaRepositorySqlServerAdapter.class);

    static final String SQL_FIND_ESTUDIANTE_GRUPO = """
            SELECT id FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?
            """;

    static final String SQL_INSERT_ESTUDIANTE_GRUPO = """
            INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado)
            VALUES (?, ?, ?, (SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'))
            """;

    static final String SQL_FIND_ASISTENCIA = """
            SELECT id FROM dbo.Asistencia WHERE estudianteGrupo = ? AND sesion = ?
            """;

    static final String SQL_INSERT_ASISTENCIA = """
            INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (?, ?, ?)
            """;

    static final String SQL_UPSERT_DETALLE_ASISTENCIA = """
            MERGE INTO dbo.DetalleAsistencia AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?))
                AS source (id, codigo, asistencia, asistio, razonCausa, observacion, estado)
            ON (target.asistencia = source.asistencia)
            WHEN MATCHED THEN
                UPDATE SET target.asistio = source.asistio,
                           target.observacion = source.observacion,
                           target.estado = source.estado,
                           target.fechaHoraFin = CURRENT_TIMESTAMP
            WHEN NOT MATCHED THEN
                INSERT (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin, observacion, estado)
                VALUES (source.id, source.codigo, source.asistencia, source.asistio, source.razonCausa, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, source.observacion, source.estado);
            """;

    static final String SQL_CONSULTAR_ASISTENCIAS = """
            SELECT
                asi.id AS asistencia,
                eg.estudiante AS estudiante,
                eg.grupo AS grupo,
                asi.sesion AS sesion,
                ISNULL(da.asistio, 0) AS presente,
                da.observacion AS observacion
            FROM dbo.Asistencia asi
            INNER JOIN dbo.EstudianteGrupo eg ON asi.estudianteGrupo = eg.id
            LEFT JOIN dbo.DetalleAsistencia da ON da.asistencia = asi.id
            WHERE eg.grupo = ? AND (? IS NULL OR asi.sesion = ?)
            """;

    static final String SQL_CONSULTAR_ESTUDIANTES_SIN_ASISTENCIA = """
            SELECT
                eg.estudiante AS estudiante,
                eg.grupo AS grupo
            FROM dbo.EstudianteGrupo eg
            WHERE eg.grupo = ?
            AND NOT EXISTS (
                SELECT 1 FROM dbo.Asistencia a WHERE a.estudianteGrupo = eg.id AND a.sesion = ?
            )
            """;

    static final String SQL_INSERT_SOLICITUD_REVISION = """
            INSERT INTO dbo.SolicitudRevisionAsistencia (
                id, nombre, asistencia, fecha, estado, justificacionSolicitud, justificacionRespuesta, categoria
            ) VALUES (
                ?, ?, ?, ?, (SELECT TOP 1 id FROM dbo.Estado WHERE codigo = 'PEND'), ?, '', 'Asistencia'
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public AsistenciaRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate para asistencias es obligatorio.");
    }

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }

        try {
            // 1. Obtener o crear matricula estudianteGrupo
            UUID estudianteGrupoId = jdbcTemplate.query(
                    SQL_FIND_ESTUDIANTE_GRUPO,
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    dto.getEstudiante(),
                    dto.getGrupo()
            );

            if (estudianteGrupoId == null) {
                estudianteGrupoId = UUID.randomUUID();
                jdbcTemplate.update(SQL_INSERT_ESTUDIANTE_GRUPO, estudianteGrupoId, dto.getEstudiante(), dto.getGrupo());
            }

            // 2. Obtener o crear registro en Asistencia
            UUID asistenciaId = jdbcTemplate.query(
                    SQL_FIND_ASISTENCIA,
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    estudianteGrupoId,
                    dto.getSesion()
            );

            if (asistenciaId == null) {
                asistenciaId = UUID.randomUUID();
                jdbcTemplate.update(SQL_INSERT_ASISTENCIA, asistenciaId, estudianteGrupoId, dto.getSesion());
            }

            // 3. Upsert en DetalleAsistencia
            final boolean presente = Boolean.TRUE.equals(dto.getPresente());
            final String estado = presente ? "PRESENTE" : "AUSENTE";
            final UUID detalleId = UUID.randomUUID();
            final UUID razonCausaId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            jdbcTemplate.update(
                    SQL_UPSERT_DETALLE_ASISTENCIA,
                    detalleId,
                    1,
                    asistenciaId,
                    presente ? 1 : 0,
                    razonCausaId,
                    dto.getObservacion() != null ? dto.getObservacion() : "",
                    estado
            );
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=registrarAsistencia, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible registrar la asistencia en base de datos.", exception);
        }
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        try {
            final List<AsistenciaRepositoryProjection> result = new ArrayList<>(
                    jdbcTemplate.query(
                            SQL_CONSULTAR_ASISTENCIAS,
                            (rs, rowNum) -> new AsistenciaRepositoryProjection(
                                    UUID.fromString(rs.getString("asistencia")),
                                    UUID.fromString(rs.getString("estudiante")),
                                    UUID.fromString(rs.getString("grupo")),
                                    UUID.fromString(rs.getString("sesion")),
                                    rs.getBoolean("presente"),
                                    rs.getString("observacion")
                            ),
                            dto.getGrupo(),
                            dto.getSesion(),
                            dto.getSesion()
                    )
            );

            // Si se especificó sesión, incluir estudiantes del grupo que aún no tengan registro (como ausentes por defecto)
            if (dto.getSesion() != null) {
                final List<AsistenciaRepositoryProjection> pendientes = jdbcTemplate.query(
                        SQL_CONSULTAR_ESTUDIANTES_SIN_ASISTENCIA,
                        (rs, rowNum) -> new AsistenciaRepositoryProjection(
                                UUID.randomUUID(),
                                UUID.fromString(rs.getString("estudiante")),
                                UUID.fromString(rs.getString("grupo")),
                                dto.getSesion(),
                                false,
                                null
                        ),
                        dto.getGrupo(),
                        dto.getSesion()
                );
                result.addAll(pendientes);
            }

            return result;
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=consultarAsistenciasPorGrupo, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible consultar las asistencias de base de datos.", exception);
        }
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }

        try {
            final UUID solicitudId = UUID.randomUUID();
            final String motivo = dto.getMotivo() != null ? dto.getMotivo() : "Solicitud de revisión de asistencia";
            final String nombre = "REV-" + solicitudId.toString().substring(0, 8).toUpperCase();

            jdbcTemplate.update(
                    SQL_INSERT_SOLICITUD_REVISION,
                    solicitudId,
                    nombre,
                    dto.getAsistencia(),
                    LocalDate.now(),
                    motivo
            );
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=solicitarRevisionAsistencia, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible registrar la solicitud de revision en base de datos.", exception);
        }
    }
}
