package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.UserScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador de portal para operaciones del Estudiante restringidas a sus materias y clases matriculadas.
 */
@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudiantePortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstudiantePortalController.class);

    static final String SQL_CONSULTAR_MATERIAS_ESTUDIANTE = """
            SELECT
                matriculaId,
                materiaId AS id,
                codigo,
                nombre,
                creditos,
                grupo,
                docente,
                aula,
                horario,
                totalClases,
                asistencias,
                inasistencias
            FROM dbo.uv_estudiante_materias_consolidado
            WHERE estudianteId = ?
            ORDER BY nombre
            """;

    static final String SQL_CONSULTAR_HORARIOS_ESTUDIANTE = """
            SELECT
                id,
                codigoMateria,
                nombreMateria,
                grupo,
                dia,
                horaInicio,
                horaFin,
                aula,
                docente,
                'emerald' AS colorCategory
            FROM dbo.uv_horario_estudiante
            WHERE estudianteId = ?
            ORDER BY dia, horaInicio
            """;

    static final String SQL_CONSULTAR_PRERREQUISITOS_MATERIA = """
            SELECT 
                p.id,
                p.asignatura AS materiaId,
                req.id AS prerrequisitoId,
                req.codigo AS prerrequisitoCodigo,
                req.nombre AS prerrequisitoNombre,
                req.credito AS creditos,
                p.tipo,
                IIF(EXISTS (
                    SELECT 1 FROM dbo.EstudianteGrupo eg2 
                    JOIN dbo.Grupo g2 ON eg2.grupo = g2.id 
                    WHERE eg2.estudiante = ? AND g2.asignatura = req.id
                ), 'APROBADA', 'PENDIENTE') AS estadoAcademico
            FROM dbo.PrerrequisitoAsignatura p
            INNER JOIN dbo.Asignatura req ON p.asignaturaRequisito = req.id
            INNER JOIN dbo.Grupo g ON g.asignatura = p.asignatura
            WHERE g.id = ?
            ORDER BY req.nombre
            """;

    static final String SQL_CONSULTAR_SESIONES_MATERIA = """
            SELECT
                s.id,
                s.grupo AS materiaId,
                s.numero AS numeroSesion,
                CONVERT(VARCHAR(10), s.fechaHoraInicio, 120) AS fecha,
                CONCAT(CONVERT(VARCHAR(5), s.fechaHoraInicio, 108), ' - ', CONVERT(VARCHAR(5), s.fechaHoraFin, 108)) AS horario,
                s.nombre AS tema,
                ISNULL(da.estado, 'AUSENTE') AS estadoAsistencia,
                sra.id AS reclamoId,
                est_sol.nombre AS estadoReclamo,
                sra.categoria AS categoriaReclamo,
                sra.justificacionSolicitud AS justificacionEstudiante,
                sra.soporteAdjuntoNombre,
                sra.justificacionRespuesta AS respuestaDocente
            FROM dbo.Sesion s
            INNER JOIN dbo.EstudianteGrupo eg ON eg.grupo = s.grupo
            LEFT JOIN dbo.Asistencia ast ON ast.sesion = s.id AND ast.estudianteGrupo = eg.id
            LEFT JOIN dbo.DetalleAsistencia da ON da.asistencia = ast.id
            LEFT JOIN dbo.SolicitudRevisionAsistencia sra ON sra.asistencia = ast.id
            LEFT JOIN dbo.Estado est_sol ON sra.estado = est_sol.id
            WHERE eg.estudiante = ? AND s.grupo = ?
            ORDER BY s.numero
            """;

    static final String SQL_CONSULTAR_RECLAMOS_ESTUDIANTE = """
            SELECT
                sra.id,
                sra.nombre AS codigo,
                CONVERT(VARCHAR(10), sra.fecha, 120) AS fechaSolicitud,
                s.nombre AS sesionNombre,
                s.numero AS sesionNumero,
                g.nombre AS cursoNombre,
                a.codigo AS cursoCodigo,
                sra.categoria,
                sra.justificacionSolicitud,
                sra.justificacionRespuesta,
                ISNULL(est_sol.nombre, 'PENDIENTE') AS estado,
                sra.soporteAdjuntoNombre,
                sra.soporteAdjuntoUrl
            FROM dbo.SolicitudRevisionAsistencia sra
            INNER JOIN dbo.Asistencia ast ON sra.asistencia = ast.id
            INNER JOIN dbo.EstudianteGrupo eg ON ast.estudianteGrupo = eg.id
            INNER JOIN dbo.Sesion s ON ast.sesion = s.id
            INNER JOIN dbo.Grupo g ON s.grupo = g.id
            INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
            LEFT JOIN dbo.Estado est_sol ON sra.estado = est_sol.id
            WHERE eg.estudiante = ?
            ORDER BY sra.fecha DESC
            """;

    static final String SQL_FIND_ASISTENCIA_DE_SESION_ESTUDIANTE = """
            SELECT ast.id
            FROM dbo.Asistencia ast
            INNER JOIN dbo.EstudianteGrupo eg ON ast.estudianteGrupo = eg.id
            WHERE eg.estudiante = ? AND ast.sesion = ?
            """;

    static final String SQL_CREATE_ASISTENCIA_SI_FALTA = """
            INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion)
            VALUES (?, (SELECT TOP 1 id FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?), ?);
            """;

    static final String SQL_INSERT_RECLAMO = """
            INSERT INTO dbo.SolicitudRevisionAsistencia (
                id, nombre, asistencia, fecha, estado, justificacionSolicitud, justificacionRespuesta,
                categoria, soporteAdjuntoNombre, soporteAdjuntoUrl
            ) VALUES (
                ?, ?, ?, ?, (SELECT TOP 1 id FROM dbo.Estado WHERE codigo = 'PEND'), ?, '', ?, ?, ?
            );
            """;

    static final String SQL_CHECK_RECLAMO_OWNERSHIP = """
            SELECT eg.estudiante
            FROM dbo.SolicitudRevisionAsistencia sra
            INNER JOIN dbo.Asistencia ast ON sra.asistencia = ast.id
            INNER JOIN dbo.EstudianteGrupo eg ON ast.estudianteGrupo = eg.id
            WHERE sra.id = ?
            """;

    static final String SQL_DELETE_RECLAMO = """
            DELETE FROM dbo.SolicitudRevisionAsistencia WHERE id = ?
            """;

    static final String SQL_INSERT_SOLICITUD_MATRICULA = """
            INSERT INTO dbo.SolicitudMatricula (
                id, estudiante, grupo, fechaSolicitud, motivo, estado, respuestaCoordinador, fechaRespuesta
            ) VALUES (
                ?, ?, ?, ?, ?, 'PENDIENTE', '', NULL
            );
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;

    public EstudiantePortalController(
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.userScopeService = Objects.requireNonNull(userScopeService, "UserScopeService es obligatorio.");
    }

    @GetMapping("/materias")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarMateriasEstudiante() {
        final UUID estudianteId = resolveCurrentEstudianteId();

        try {
            final List<Map<String, Object>> raw = jdbcTemplate.queryForList(SQL_CONSULTAR_MATERIAS_ESTUDIANTE, estudianteId);
            final List<Map<String, Object>> processed = new ArrayList<>();

            for (final Map<String, Object> row : raw) {
                final Map<String, Object> item = new HashMap<>(row);
                final int totalClases = ((Number) row.getOrDefault("totalClases", 0)).intValue();
                final int asistencias = ((Number) row.getOrDefault("asistencias", 0)).intValue();
                final int inasistencias = ((Number) row.getOrDefault("inasistencias", 0)).intValue();

                final int totalTomadas = asistencias + inasistencias;
                final double porcentaje = totalTomadas > 0
                        ? Math.round(((double) asistencias / totalTomadas) * 100.0)
                        : 100.0;

                final String estado;
                if (porcentaje >= 80.0) {
                    estado = "Al día";
                } else if (porcentaje >= 75.0) {
                    estado = "Riesgo";
                } else {
                    estado = "Crítico";
                }

                item.put("porcentajeAsistencia", porcentaje);
                item.put("estado", estado);
                processed.add(item);
            }

            return ResponseEntity.ok(new ApiListResponse<>(true, processed, processed.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando materias para estudianteId={}", estudianteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarHorariosEstudiante() {
        final UUID estudianteId = resolveCurrentEstudianteId();

        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_HORARIOS_ESTUDIANTE, estudianteId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando horarios para estudianteId={}", estudianteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @GetMapping("/materias/{materiaId}/sesiones")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarSesionesPorMateria(
            @PathVariable final UUID materiaId
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();

        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    SQL_CONSULTAR_SESIONES_MATERIA, estudianteId, materiaId
            );
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando sesiones para estudianteId={} materiaId={}", estudianteId, materiaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @GetMapping("/materias/{materiaId}/prerrequisitos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarPrerrequisitosMateria(
            @PathVariable final UUID materiaId
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();

        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    SQL_CONSULTAR_PRERREQUISITOS_MATERIA, estudianteId, materiaId
            );
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando prerrequisitos para estudianteId={} materiaId={}", estudianteId, materiaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarReclamosEstudiante() {
        final UUID estudianteId = resolveCurrentEstudianteId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_RECLAMOS_ESTUDIANTE, estudianteId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando reclamos para estudianteId={}", estudianteId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/reclamos")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearReclamo(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();
        final UUID sesionId = UUID.fromString(payload.get("sesionId").toString());
        final String categoria = Objects.toString(payload.get("categoria"), "Médico / Salud");
        final String justificacion = Objects.toString(payload.get("justificacionSolicitud"), "");
        final String soporteNombre = payload.get("soporteAdjunto") instanceof Map<?, ?> m
                ? Objects.toString(((Map<?, ?>) m).get("nombre"), null) : null;
        final String soporteUrl = payload.get("soporteAdjunto") instanceof Map<?, ?> m
                ? Objects.toString(((Map<?, ?>) m).get("urlSimulada"), null) : null;
        final UUID correlacion = UUID.randomUUID();

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_radicar_solicitud_revision_asistencia @idEstudiante = ?, @idSesion = ?, @categoria = ?, @justificacion = ?, @soporteNombre = ?, @soporteUrl = ?, @idCorrelacion = ?, @idSolicitudResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    estudianteId, sesionId, categoria, justificacion, soporteNombre, soporteUrl, correlacion
            );

            final Boolean exitoso = (Boolean) spResult.getOrDefault("exitoso", true);
            if (Boolean.FALSE.equals(exitoso)) {
                final String mensaje = (String) spResult.getOrDefault("mensajeUsuario", "No tiene autorización para radicar reclamos de una materia no matriculada.");
                throw new ForbiddenException(mensaje);
            }

            final String idSolicitud = Objects.toString(spResult.get("idSolicitud"), UUID.randomUUID().toString());
            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", idSolicitud);
            result.put("fechaSolicitud", LocalDate.now().toString());
            result.put("estadoSolicitud", "PENDIENTE");

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al ejecutar usp_radicar_solicitud_revision_asistencia", ex);
            final UUID materiaId = UUID.fromString(payload.get("materiaId").toString());
            UUID asistenciaId = jdbcTemplate.query(
                    SQL_FIND_ASISTENCIA_DE_SESION_ESTUDIANTE,
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    estudianteId,
                    sesionId
            );

            if (asistenciaId == null) {
                asistenciaId = UUID.randomUUID();
                jdbcTemplate.update(SQL_CREATE_ASISTENCIA_SI_FALTA, asistenciaId, estudianteId, materiaId, sesionId);
            }

            final UUID id = UUID.randomUUID();
            final String nombre = "REC-" + id.toString().substring(0, 8).toUpperCase();
            final String fecha = LocalDate.now().toString();

            jdbcTemplate.update(
                    SQL_INSERT_RECLAMO,
                    id,
                    nombre,
                    asistenciaId,
                    fecha,
                    justificacion,
                    categoria,
                    soporteNombre,
                    soporteUrl
            );

            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", id.toString());
            result.put("fechaSolicitud", fecha);
            result.put("estadoSolicitud", "PENDIENTE");

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
        }
    }

    @DeleteMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Boolean>> eliminarReclamo(
            @PathVariable final UUID id
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();

        final UUID estudianteOwner = jdbcTemplate.query(
                SQL_CHECK_RECLAMO_OWNERSHIP,
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                id
        );

        if (estudianteOwner == null) {
            throw new ResourceNotFoundException("Solicitud no encontrada.");
        }

        if (!estudianteOwner.equals(estudianteId)) {
            throw new ForbiddenException("No puede anular una solicitud que no le pertenece.");
        }

        jdbcTemplate.update(SQL_DELETE_RECLAMO, id);
        return ResponseEntity.ok(new ApiDataResponse<>(true, true));
    }

    @PostMapping("/solicitudes-matricula")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearSolicitudMatricula(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();
        final UUID grupoId = UUID.fromString(payload.get("cursoId").toString());
        final String motivo = Objects.toString(payload.get("motivo"), "Solicitud de inscripción por cupo.");
        final UUID id = UUID.randomUUID();
        final LocalDate now = LocalDate.now();

        jdbcTemplate.update(
                SQL_INSERT_SOLICITUD_MATRICULA,
                id,
                estudianteId,
                grupoId,
                now,
                motivo
        );

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", id.toString());
        result.put("fechaSolicitud", now.toString());
        result.put("estado", "PENDIENTE");

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PostMapping("/matricular-grupo")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> autoMatricularGrupo(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID estudianteId = resolveCurrentEstudianteId();
        final String codigoOPin = Objects.toString(payload.get("codigo"),
                Objects.toString(payload.get("pin"),
                        Objects.toString(payload.get("grupoId"), ""))).trim();

        if (codigoOPin.isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar el código o PIN del grupo.");
        }

        // Buscar grupo por UUID directo o por su código numérico institucional
        final String sqlBuscarGrupo = """
                SELECT TOP 1 g.id, g.codigo, g.nombre, a.nombre AS nombreAsignatura
                FROM dbo.Grupo g
                INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
                WHERE CAST(g.id AS VARCHAR(36)) = ?
                   OR CAST(g.codigo AS VARCHAR(20)) = ?
                   OR g.nombre = ?
                """;

        final Map<String, Object> grupo = jdbcTemplate.query(
                sqlBuscarGrupo,
                rs -> {
                    if (rs.next()) {
                        return Map.of(
                                "id", UUID.fromString(rs.getString("id")),
                                "codigo", rs.getInt("codigo"),
                                "nombre", rs.getString("nombre"),
                                "nombreAsignatura", rs.getString("nombreAsignatura")
                        );
                    }
                    return null;
                },
                codigoOPin, codigoOPin, codigoOPin
        );

        if (grupo == null) {
            throw new ResourceNotFoundException("No se encontró ningún grupo académico con el código o PIN proporcionado.");
        }

        final UUID grupoId = (UUID) grupo.get("id");

        // Validar si ya se encuentra matriculado
        final Integer matriculado = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                Integer.class, estudianteId, grupoId
        );

        if (matriculado != null && matriculado > 0) {
            throw new ConflictException("Ya te encuentras matriculado en este grupo académico.");
        }

        // Matricular al estudiante en dbo.EstudianteGrupo
        final UUID estadoActivoId = jdbcTemplate.query(
                "SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
        );

        final UUID nuevaMatriculaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado) VALUES (?, ?, ?, ?)",
                nuevaMatriculaId, estudianteId, grupoId, estadoActivoId
        );

        final Map<String, Object> respuesta = Map.of(
                "matriculaId", nuevaMatriculaId.toString(),
                "grupoId", grupoId.toString(),
                "grupoNombre", grupo.get("nombre"),
                "asignaturaNombre", grupo.get("nombreAsignatura"),
                "mensajeUsuario", String.format("¡Te has matriculado exitosamente en el grupo '%s' (%s)!",
                        grupo.get("nombre"), grupo.get("nombreAsignatura"))
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, respuesta));
    }

    private UUID resolveCurrentEstudianteId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000004")); // Fallback estudiante de prueba

        return userScopeService.findEstudianteIdByUsuario(usuarioId)
                .orElse(UUID.fromString("F1A2B3C4-0000-0000-0000-000000000004"));
    }
}
