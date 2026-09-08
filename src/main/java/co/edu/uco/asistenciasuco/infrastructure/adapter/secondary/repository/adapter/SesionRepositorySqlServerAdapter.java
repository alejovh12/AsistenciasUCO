package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server para persistencia y consulta de sesiones de clase.
 */
public final class SesionRepositorySqlServerAdapter implements SesionRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesionRepositorySqlServerAdapter.class);

    static final String SQL_INSERTAR_SESION = """
            INSERT INTO dbo.Sesion (
                id, nombre, numero, codigo, numeroSemana, grupo,
                fechaHoraInicio, fechaHoraFin, aula, tipo, descripcion, cerrada
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """;

    static final String SQL_COUNT_SESIONES_GRUPO = """
            SELECT COUNT(1) FROM dbo.Sesion WHERE grupo = ?
            """;

    static final String SQL_CONSULTAR_POR_ID = """
            SELECT id, grupo, nombre, descripcion, cerrada
            FROM dbo.Sesion
            WHERE id = ?
            """;

    static final String SQL_CERRAR_SESION = """
            UPDATE dbo.Sesion
            SET cerrada = 1
            WHERE id = ?
            """;

    static final String SQL_CHECK_DOCENTE_SCHEDULE_COLLISION = """
            SELECT COUNT(1)
            FROM dbo.Sesion s
            INNER JOIN dbo.Grupo g ON s.grupo = g.id
            WHERE g.docente = (SELECT docente FROM dbo.Grupo WHERE id = ?)
              AND s.cerrada = 0
              AND s.fechaHoraInicio < ?
              AND s.fechaHoraFin > ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public SesionRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate para sesiones es obligatorio.");
    }

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }

        try {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime end = now.plusHours(2);

            // Obtener el docente titular del grupo para validación de ámbito
            final UUID docenteId = jdbcTemplate.query(
                    "SELECT docente FROM dbo.Grupo WHERE id = ?",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    dto.getGrupo()
            );

            final String nombre = (dto.getTema() != null && !dto.getTema().isBlank())
                    ? dto.getTema()
                    : "Sesión Regular";
            final String descripcion = dto.getDescripcion() != null ? dto.getDescripcion() : nombre;
            final UUID correlacion = CorrelationIdContext.get();

            if (docenteId != null) {
                // Invocación del procedimiento institucional almacenado dbo.usp_crear_sesion
                final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                        "EXEC dbo.usp_crear_sesion @idGrupo = ?, @idDocente = ?, @nombre = ?, @descripcion = ?, @fechaHoraInicio = ?, @fechaHoraFin = ?, @aula = 'Aula Asignada', @tipo = 'REGULAR', @idCorrelacion = ?, @idSesionResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                        dto.getGrupo(), docenteId, nombre, descripcion, now, end, correlacion
                );

                final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                        : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
                final String mensaje = Objects.toString(spResult.get("mensajeUsuario"), "Operación exitosa.");

                if (!exitoso) {
                    if (mensaje.contains("Acceso denegado") || mensaje.contains("ámbito")) {
                        throw new co.edu.uco.asistenciasuco.application.exception.business.ConflictException(mensaje);
                    }
                    throw new ValidationException(mensaje);
                }
            } else {
                // Fallback directo si el grupo no tiene docente asignado aún
                final Integer existingCount = jdbcTemplate.queryForObject(
                        SQL_COUNT_SESIONES_GRUPO, Integer.class, dto.getGrupo()
                );
                final int nextNumber = (existingCount != null ? existingCount : 0) + 1;
                final UUID sesionId = UUID.randomUUID();
                final String codigo = "SES-" + String.format("%02d", nextNumber);

                jdbcTemplate.update(
                        SQL_INSERTAR_SESION,
                        sesionId,
                        nombre,
                        nextNumber,
                        codigo,
                        nextNumber,
                        dto.getGrupo(),
                        now,
                        end,
                        "Aula Asignada",
                        "REGULAR",
                        descripcion
                );
            }
        } catch (co.edu.uco.asistenciasuco.application.exception.business.ConflictException ex) {
            throw ex;
        } catch (ValidationException ex) {
            throw ex;
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=crearSesion, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible registrar la sesion en base de datos.", exception);
        }
    }

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        try {
            return jdbcTemplate.query(
                    SQL_CONSULTAR_POR_ID,
                    rs -> {
                        if (rs.next()) {
                            final UUID id = UUID.fromString(rs.getString("id"));
                            final UUID grupo = UUID.fromString(rs.getString("grupo"));
                            final String nombre = rs.getString("nombre");
                            final String descripcion = rs.getString("descripcion");
                            final boolean cerrada = rs.getBoolean("cerrada");
                            final String obs = cerrada ? "Sesion concluida." : null;
                            return new SesionRepositoryProjection(id, grupo, nombre, descripcion, cerrada, obs);
                        }
                        return null;
                    },
                    dto.getSesion()
            );
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=consultarSesion, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible consultar la sesion desde base de datos.", exception);
        }
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }

        try {
            final UUID docenteId = jdbcTemplate.query(
                    "SELECT g.docente FROM dbo.Sesion s INNER JOIN dbo.Grupo g ON s.grupo = g.id WHERE s.id = ?",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    dto.getSesion()
            );

            if (docenteId != null) {
                final UUID correlacion = CorrelationIdContext.get();
                jdbcTemplate.update(
                        "EXEC dbo.usp_cerrar_sesion @idSesion = ?, @idDocente = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                        dto.getSesion(), docenteId, correlacion
                );
            } else {
                jdbcTemplate.update(SQL_CERRAR_SESION, dto.getSesion());
            }
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation=cerrarSesion, correlationId={}",
                    CorrelationIdContext.getAsString(), exception);
            throw new DatabaseOperationException("No fue posible cerrar la sesion en base de datos.", exception);
        }
    }
}
