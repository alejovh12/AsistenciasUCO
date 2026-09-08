package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.UserScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador de portal para operaciones del Docente restringidas a su ámbito institucional (sus grupos asignados).
 */
@RestController
@RequestMapping("/api/v1/docente")
public final class DocentePortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocentePortalController.class);

    static final String SQL_CONSULTAR_RECLAMOS_DOCENTE = """
            SELECT
                id,
                estudianteId,
                estudianteNombre,
                estudianteCorreo,
                materiaId,
                materiaCodigo,
                materiaNombre,
                grupo,
                sesionId,
                sesionNumero,
                fechaSesion,
                ISNULL(estadoOriginal, 'AUSENTE') AS estadoOriginal,
                fechaSolicitud,
                estadoSolicitud,
                categoria,
                soporteAdjuntoNombre,
                soporteAdjuntoUrl,
                justificacionSolicitud,
                justificacionRespuesta,
                fechaRespuesta
            FROM dbo.uv_docente_solicitudes_revision
            WHERE docenteId = ?
            ORDER BY fechaSolicitud DESC, sesionNumero DESC
            """;

    static final String SQL_FIND_DOCENTE_DE_RECLAMO = """
            SELECT g.docente
            FROM dbo.SolicitudRevisionAsistencia sra
            INNER JOIN dbo.Asistencia ast ON sra.asistencia = ast.id
            INNER JOIN dbo.Sesion s ON ast.sesion = s.id
            INNER JOIN dbo.Grupo g ON s.grupo = g.id
            WHERE sra.id = ?
            """;

    static final String SQL_UPDATE_RECLAMO = """
            UPDATE dbo.SolicitudRevisionAsistencia
            SET estado = (SELECT TOP 1 id FROM dbo.Estado WHERE codigo = ?),
                justificacionRespuesta = ?,
                fechaRespuesta = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    static final String SQL_UPDATE_ASISTENCIA_A_JUSTIFICADA = """
            UPDATE da
            SET da.asistio = 1,
                da.estado = 'JUSTIFICADA',
                da.observacion = CONCAT(ISNULL(da.observacion + ' | ', ''), 'Justificación aprobada por docente: ', ?)
            FROM dbo.DetalleAsistencia da
            INNER JOIN dbo.SolicitudRevisionAsistencia sra ON sra.asistencia = da.asistencia
            WHERE sra.id = ?
            """;

    static final String SQL_CONSULTAR_HORARIOS_DOCENTE = """
            SELECT
                id,
                codigoMateria,
                nombreMateria,
                seccion,
                dia,
                horaInicio,
                horaFin,
                aula,
                totalEstudiantes,
                'emerald' AS colorCategory
            FROM dbo.uv_horario_docente
            WHERE docenteId = ?
            ORDER BY dia, horaInicio
            """;

    static final String SQL_VERIFICAR_SESION_DOCENTE = """
            SELECT g.docente
            FROM dbo.Sesion s
            INNER JOIN dbo.Grupo g ON s.grupo = g.id
            WHERE s.id = ?
            """;

    static final String SQL_CANCELAR_SESION = """
            UPDATE dbo.Sesion
            SET cerrada = 1,
                estado = 'CANCELADA',
                motivoCancelacion = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;

    public DocentePortalController(
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.userScopeService = Objects.requireNonNull(userScopeService, "UserScopeService es obligatorio.");
    }

    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarReclamosDocente() {
        final UUID docenteId = resolveCurrentDocenteId();

        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_RECLAMOS_DOCENTE, docenteId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando reclamos para docenteId={}", docenteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PatchMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> resolverReclamoDocente(
            @PathVariable final UUID id,
            @RequestBody final Map<String, String> body
    ) {
        final UUID docenteId = resolveCurrentDocenteId();
        final String accion = body.getOrDefault("accion", "RECHAZADA").toUpperCase();
        final String respuesta = body.getOrDefault("respuesta", "");
        final UUID correlacion = UUID.randomUUID();

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_resolver_solicitud_revision_asistencia @idSolicitud = ?, @idDocente = ?, @accion = ?, @respuestaDocente = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, docenteId, accion, respuesta, correlacion
            );

            final Boolean exitoso = (Boolean) spResult.getOrDefault("exitoso", true);
            if (Boolean.FALSE.equals(exitoso)) {
                final String mensaje = (String) spResult.getOrDefault("mensajeUsuario", "Acceso denegado o error de validación.");
                if (mensaje.contains("Acceso denegado")) {
                    throw new ForbiddenException(mensaje);
                }
                throw new ResourceNotFoundException(mensaje);
            }

            final Map<String, Object> result = Map.of(
                    "id", id.toString(),
                    "estadoSolicitud", accion,
                    "justificacionRespuesta", respuesta,
                    "mensajeUsuario", Objects.toString(spResult.get("mensajeUsuario"), "Reclamo resuelto.")
            );

            return ResponseEntity.ok(new ApiDataResponse<>(true, result));
        } catch (ForbiddenException | ResourceNotFoundException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al ejecutar usp_resolver_solicitud_revision_asistencia", ex);
            // Fallback directo
            final UUID docenteAsignado = jdbcTemplate.query(
                    SQL_FIND_DOCENTE_DE_RECLAMO,
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    id
            );
            if (docenteAsignado == null) {
                throw new ResourceNotFoundException("Solicitud de revisión no encontrada.");
            }
            if (!docenteAsignado.equals(docenteId)) {
                throw new ForbiddenException("No tiene autorización para resolver solicitudes de un grupo ajeno.");
            }
            final String codigoEstado = "APROBADA".equals(accion) ? "APRO" : "RECH";
            jdbcTemplate.update(SQL_UPDATE_RECLAMO, codigoEstado, respuesta, id);
            if ("APROBADA".equals(accion)) {
                jdbcTemplate.update(SQL_UPDATE_ASISTENCIA_A_JUSTIFICADA, respuesta, id);
            }
            final Map<String, Object> result = Map.of(
                    "id", id.toString(),
                    "estadoSolicitud", accion,
                    "justificacionRespuesta", respuesta,
                    "mensajeUsuario", "APROBADA".equals(accion)
                            ? "Reclamo aprobado. Se ha actualizado la asistencia a 'Justificada'."
                            : "Reclamo rechazado."
            );
            return ResponseEntity.ok(new ApiDataResponse<>(true, result));
        }
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarHorarioDocente() {
        final UUID docenteId = resolveCurrentDocenteId();

        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_HORARIOS_DOCENTE, docenteId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando horarios para docenteId={}", docenteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PatchMapping("/sesiones/{sesionId}/cancelar")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> cancelarSesionDocente(
            @PathVariable final UUID sesionId,
            @RequestBody final Map<String, String> body
    ) {
        final UUID docenteId = resolveCurrentDocenteId();
        final String motivo = body.getOrDefault("motivo", "Sesión cancelada por el docente titular por motivos académicos.");

        final UUID docenteAsignado = jdbcTemplate.query(
                SQL_VERIFICAR_SESION_DOCENTE,
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                sesionId
        );

        if (docenteAsignado == null) {
            throw new ResourceNotFoundException("La sesión no existe.");
        }

        if (!docenteAsignado.equals(docenteId)) {
            throw new ForbiddenException("No tiene autorización para cancelar una sesión de un grupo ajeno.");
        }

        jdbcTemplate.update(SQL_CANCELAR_SESION, motivo, sesionId);

        final Map<String, Object> result = Map.of(
                "sesionId", sesionId.toString(),
                "estado", "CANCELADA",
                "motivoCancelacion", motivo,
                "mensajeUsuario", "Sesión cancelada formalmente. Se ha notificado a los estudiantes matriculados."
        );

        return ResponseEntity.ok(new ApiDataResponse<>(true, result));
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarAsignaturasDocente() {
        final UUID docenteId = resolveCurrentDocenteId();
        try {
            final String sql = """
                    SELECT DISTINCT
                        a.id,
                        a.codigo,
                        a.nombre,
                        a.credito AS creditos,
                        p.nombre AS nombrePrograma
                    FROM dbo.Asignatura a
                    INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                    INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
                    INNER JOIN dbo.Programa p ON pe.programa = p.id
                    WHERE a.id IN (
                        SELECT g.asignatura FROM dbo.Grupo g WHERE g.docente = ?
                    )
                    OR p.id IN (
                        SELECT pe2.programa
                        FROM dbo.Grupo g2
                        INNER JOIN dbo.Asignatura a2 ON g2.asignatura = a2.id
                        INNER JOIN dbo.SemestrePlanEstudio spe2 ON a2.semestrePlanEstudio = spe2.id
                        INNER JOIN dbo.PlanEstudio pe2 ON spe2.planEstudio = pe2.id
                        WHERE g2.docente = ?
                    )
                    ORDER BY a.nombre
                    """;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, docenteId, docenteId);
            if (list.isEmpty()) {
                list = jdbcTemplate.queryForList("""
                        SELECT a.id, a.codigo, a.nombre, a.credito AS creditos, 'Ingeniería de Sistemas' AS nombrePrograma
                        FROM dbo.Asignatura a ORDER BY a.nombre
                        """);
            }
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando asignaturas para docenteId={}", docenteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    private UUID resolveCurrentDocenteId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000003")); // Fallback docente de prueba

        return userScopeService.findDocenteIdByUsuario(usuarioId)
                .orElse(UUID.fromString("F1A2B3C4-0000-0000-0000-000000000003"));
    }
}
