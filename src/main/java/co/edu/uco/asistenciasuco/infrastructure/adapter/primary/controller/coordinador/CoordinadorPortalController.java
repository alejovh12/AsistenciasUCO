package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * Controlador de portal para operaciones del Coordinador restringidas a su Programa Académico.
 */
@RestController
@RequestMapping("/api/v1/coordinador")
public final class CoordinadorPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinadorPortalController.class);

    static final String SQL_CONSULTAR_DOCENTES_PROGRAMA = """
            SELECT DISTINCT
                d.id,
                u.numeroIdentificacion,
                u.primerNombre AS nombres,
                CONCAT(u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS apellidos,
                u.correo,
                ISNULL(p.nombre, 'Ingeniería de Sistemas') AS departamento,
                'Docente de Programa' AS especialidad,
                (SELECT COUNT(1) FROM dbo.Grupo g WHERE g.docente = d.id) AS totalGruposAsignados,
                IIF(u.estado = 1, 'ACTIVO', 'INACTIVO') AS estado
            FROM dbo.Docente d
            INNER JOIN dbo.Usuario u ON d.usuario = u.id
            LEFT JOIN dbo.Grupo g ON g.docente = d.id
            LEFT JOIN dbo.Asignatura a ON g.asignatura = a.id
            LEFT JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
            LEFT JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
            LEFT JOIN dbo.Programa p ON pe.programa = p.id
            WHERE p.id = ? OR p.id IS NULL
            """;

    static final String SQL_CONSULTAR_PLANES_PROGRAMA = """
            SELECT
                pe.id,
                pe.codigo,
                pe.nombre,
                pe.inp AS anioVigencia,
                p.nombre AS programa,
                f.nombre AS facultad,
                pe.totalCreditos,
                pe.totalSemestres,
                (SELECT COUNT(1) FROM dbo.Asignatura a 
                 INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id 
                 WHERE spe.planEstudio = pe.id) AS totalAsignaturas,
                IIF(pe.estado = 1, 'VIGENTE', 'INACTIVO') AS estado,
                CONCAT('Plan de estudios correspondiente a ', p.nombre) AS descripcion
            FROM dbo.PlanEstudio pe
            INNER JOIN dbo.Programa p ON pe.programa = p.id
            INNER JOIN dbo.Facultad f ON p.facultad = f.id
            WHERE p.id = ?
            ORDER BY pe.codigo
            """;

    static final String SQL_CONSULTAR_ASIGNATURAS_PLAN = """
            SELECT
                a.id,
                spe.planEstudio AS planEstudioId,
                a.codigo,
                a.nombre,
                a.credito AS creditos,
                s.numero AS semestre,
                ISNULL(ar.nombre, 'Ciencias Computacionales') AS area,
                ISNULL(c.nombre, 'Obligatoria') AS componente,
                4 AS horasSemanales,
                IIF(a.estado = 1, 'ACTIVO', 'INACTIVO') AS estado
            FROM dbo.Asignatura a
            INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
            INNER JOIN dbo.Semestre s ON spe.semestre = s.id
            LEFT JOIN dbo.Area ar ON a.area = ar.id
            LEFT JOIN dbo.Componente c ON a.componente = c.id
            WHERE spe.planEstudio = ?
            ORDER BY s.numero, a.nombre
            """;

    static final String SQL_CONSULTAR_PRERREQUISITOS_PLAN = """
            SELECT
                pa.asignatura AS asignaturaId,
                req.codigo AS codigoRequisito
            FROM dbo.PrerrequisitoAsignatura pa
            INNER JOIN dbo.Asignatura req ON pa.asignaturaRequisito = req.id
            INNER JOIN dbo.Asignatura a ON pa.asignatura = a.id
            INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
            WHERE spe.planEstudio = ?
            """;

    static final String SQL_CONSULTAR_PERIODOS = """
            SELECT
                id,
                codigo,
                nombre,
                CONVERT(VARCHAR(10), fechaInicio, 120) AS fechaInicio,
                CONVERT(VARCHAR(10), fechaFin, 120) AS fechaFin,
                CONVERT(VARCHAR(10), fechaFin, 120) AS fechaLimiteNotas,
                IIF(estado = 1, 'ACTIVO', 'INACTIVO') AS estado,
                1 AS esActual
            FROM dbo.PeriodoAcademico
            ORDER BY fechaInicio DESC
            """;

    static final String SQL_CONSULTAR_ESTUDIANTES_PROGRAMA = """
            SELECT
                id,
                documento,
                codigo,
                nombreCompleto,
                correo,
                programa,
                semestreActual,
                estadoMatricula
            FROM dbo.uv_coordinador_directorio_estudiantes
            WHERE programaId = ?
            ORDER BY nombreCompleto
            """;

    static final String SQL_CONSULTAR_SOLICITUDES_MATRICULA = """
            SELECT
                sm.id,
                sm.estudiante AS estudianteId,
                CONCAT(ue.primerNombre, ' ', ISNULL(ue.segundoNombre + ' ', ''), ue.primerApellido, ' ', ISNULL(ue.segundoApellido, '')) AS estudianteNombre,
                CAST(ue.numeroIdentificacion AS VARCHAR(20)) AS estudianteCodigo,
                ue.correo AS estudianteCorreo,
                g.id AS cursoId,
                a.codigo AS cursoCodigo,
                a.nombre AS cursoNombre,
                g.nombre AS grupo,
                CONVERT(VARCHAR(10), sm.fechaSolicitud, 120) AS fechaSolicitud,
                sm.motivo,
                sm.estado,
                sm.respuestaCoordinador,
                CONVERT(VARCHAR(10), sm.fechaRespuesta, 120) AS fechaRespuesta
            FROM dbo.SolicitudMatricula sm
            INNER JOIN dbo.Estudiante e ON sm.estudiante = e.id
            INNER JOIN dbo.Usuario ue ON e.usuario = ue.id
            INNER JOIN dbo.Grupo g ON sm.grupo = g.id
            INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
            INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
            INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
            WHERE pe.programa = ?
            ORDER BY sm.fechaSolicitud DESC
            """;

    static final String SQL_INSERT_PLAN = """
            INSERT INTO dbo.PlanEstudio (id, codigo, nombre, totalCreditos, totalSemestres, inp, programa, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1);
            """;

    static final String SQL_UPDATE_PLAN = """
            UPDATE dbo.PlanEstudio
            SET codigo = ?, nombre = ?, totalCreditos = ?, totalSemestres = ?, inp = ?
            WHERE id = ? AND programa = ?;
            """;

    static final String SQL_TOGGLE_ESTADO_PLAN = """
            UPDATE dbo.PlanEstudio
            SET estado = IIF(estado = 1, 0, 1)
            WHERE id = ? AND programa = ?;
            """;

    static final String SQL_INSERT_ASIGNATURA = """
            INSERT INTO dbo.Asignatura (id, nombre, codigo, credito, semestrePlanEstudio, componente)
            VALUES (?, ?, ?, ?, ?, (SELECT TOP 1 id FROM dbo.Componente));
            """;

    static final String SQL_DELETE_ASIGNATURA = """
            DELETE FROM dbo.Asignatura WHERE id = ?;
            """;

    static final String SQL_RESOLVER_SOLICITUD_MATRICULA = """
            UPDATE dbo.SolicitudMatricula
            SET estado = ?, respuestaCoordinador = ?, fechaRespuesta = CURRENT_TIMESTAMP
            WHERE id = ?;
            """;

    static final String SQL_ENROLL_ESTUDIANTE_GRUPO = """
            IF NOT EXISTS (SELECT 1 FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?)
            BEGIN
                INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado, fechaInscripcion)
                VALUES (NEWID(), ?, ?, (SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'INS'), CURRENT_TIMESTAMP);
            END
            """;

    static final String SQL_RETIRAR_ESTUDIANTE_GRUPO = """
            DELETE FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?;
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;
    private final co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime.RealtimeEventHub realtimeEventHub;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public CoordinadorPortalController(
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime.RealtimeEventHub realtimeEventHub,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.userScopeService = Objects.requireNonNull(userScopeService, "UserScopeService es obligatorio.");
        this.realtimeEventHub = Objects.requireNonNull(realtimeEventHub, "RealtimeEventHub es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @GetMapping("/docentes")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarDocentesPrograma() {
        final UUID programaId = resolveCurrentProgramaId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_DOCENTES_PROGRAMA, programaId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando docentes para programaId={}", programaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/docentes")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearDocente(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID programaId = resolveCurrentProgramaId();
        final UUID docenteId = UUID.randomUUID();
        final UUID usuarioId = UUID.randomUUID();

        final String rawPN = Objects.toString(payload.get("primerNombre"), "").trim();
        final String rawSN = Objects.toString(payload.get("segundoNombre"), "").trim();
        final String rawPA = Objects.toString(payload.get("primerApellido"), "").trim();
        final String rawSA = Objects.toString(payload.get("segundoApellido"), "").trim();

        final String primerNombre;
        final String segundoNombre;
        if (!rawPN.isEmpty()) {
            primerNombre = rawPN;
            segundoNombre = rawSN;
        } else {
            final String nombres = Objects.toString(payload.get("nombres"), "Docente").trim();
            final String[] nombrePartes = nombres.split("\\s+", 2);
            primerNombre = nombrePartes[0];
            segundoNombre = nombrePartes.length > 1 ? nombrePartes[1] : "";
        }

        final String primerApellido;
        final String segundoApellido;
        if (!rawPA.isEmpty()) {
            primerApellido = rawPA;
            segundoApellido = rawSA;
        } else {
            final String apellidos = Objects.toString(payload.get("apellidos"), "UCO").trim();
            final String[] apellidoPartes = apellidos.split("\\s+", 2);
            primerApellido = apellidoPartes[0];
            segundoApellido = apellidoPartes.length > 1 ? apellidoPartes[1] : "";
        }

        final String rawDoc = Objects.toString(payload.get("numeroIdentificacion"), "1017000000").replaceAll("[^0-9]", "");
        final int numeroIdentificacion = rawDoc.isEmpty() ? 1017000000 : Integer.parseInt(rawDoc);
        final String correo = Objects.toString(payload.get("correo"), "docente" + numeroIdentificacion + "@uco.edu.co").trim();
        final String departamento = Objects.toString(payload.get("departamento"), "Ingeniería de Sistemas");
        final String especialidad = Objects.toString(payload.get("especialidad"), "Docente de Programa");

        // 1. Obtener tipo de identificación CC por defecto
        final UUID tipoId = jdbcTemplate.query(
                "SELECT TOP 1 id FROM dbo.TipoIdentificacion WHERE tipoIdentificacion = 'CC'",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("A1B2C3D4-0000-0000-0000-000000000001")
        );

        // 2. Insertar en SQL Server (BD es la fuente de verdad de negocio)
        final String rawPassword = Objects.toString(payload.get("password"), "Test1234!").trim();
        final String passwordToHash = rawPassword.isEmpty() ? "Test1234!" : rawPassword;
        final String hashedPassword = passwordEncoderPort.encode(passwordToHash);

        jdbcTemplate.update(
                """
                INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, correo, correoConfirmado, estado, password)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?);
                """,
                usuarioId, tipoId, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, correo, hashedPassword
        );

        jdbcTemplate.update(
                """
                INSERT INTO dbo.Docente (id, usuario)
                VALUES (?, ?);
                """,
                docenteId, usuarioId
        );

        // 3. Crear cuenta en el proveedor de identidad para habilitar el login
        // Si el IdP falla se loguea la advertencia pero no se revierte el registro en BD
        // (el administrador puede completar la cuenta en el IdP manualmente si fuera necesario)
        String idExternoIdP = null;
        try {
            final var cuentaDTO = new CrearCuentaIdentidadDTO(
                    String.valueOf(numeroIdentificacion),
                    correo,
                    primerNombre,
                    primerApellido,
                    rawPassword.isEmpty() ? "Test1234!" : rawPassword,
                    "DOCENTE"
            );
            final var cuentaCreada = identityProviderPort.crearCuenta(cuentaDTO);
            idExternoIdP = cuentaCreada.idExterno();
            LOGGER.info("Cuenta de docente creada en IdP: username={}, idExterno={}", cuentaDTO.username(), idExternoIdP);
        } catch (IdentityProviderPort.IdentityProviderException ex) {
            LOGGER.error("No se pudo crear la cuenta del docente en el proveedor de identidad. " +
                    "El usuario existe en BD pero no podrá iniciar sesión hasta que se resuelva. " +
                    "numeroIdentificacion={}, correo={}", numeroIdentificacion, correo, ex);
        }

        final Map<String, Object> result = new HashMap<>();
        result.put("id", docenteId.toString());
        result.put("numeroIdentificacion", String.valueOf(numeroIdentificacion));
        result.put("nombres", (primerNombre + " " + segundoNombre).trim());
        result.put("apellidos", (primerApellido + " " + segundoApellido).trim());
        result.put("correo", correo);
        result.put("departamento", departamento);
        result.put("especialidad", especialidad);
        result.put("totalGruposAsignados", 0);
        result.put("estado", "ACTIVO");
        result.put("mensajeUsuario", "Docente vinculado al programa exitosamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PatchMapping("/docentes/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoDocente(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                """
                UPDATE u
                SET u.estado = IIF(u.estado = 1, 0, 1)
                FROM dbo.Usuario u
                INNER JOIN dbo.Docente d ON d.usuario = u.id
                WHERE d.id = ?;
                """,
                id
        );

        if (updated == 0) {
            throw new ResourceNotFoundException("Docente no encontrado.");
        }

        final Map<String, Object> docenteActualizado = jdbcTemplate.queryForMap(
                """
                SELECT
                    d.id,
                    CAST(u.numeroIdentificacion AS VARCHAR(20)) AS numeroIdentificacion,
                    u.primerNombre AS nombres,
                    CONCAT(u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS apellidos,
                    u.correo,
                    'Ingeniería de Sistemas' AS departamento,
                    'Docente de Programa' AS especialidad,
                    (SELECT COUNT(1) FROM dbo.Grupo g WHERE g.docente = d.id) AS totalGruposAsignados,
                    IIF(u.estado = 1, 'ACTIVO', 'INACTIVO') AS estado
                FROM dbo.Docente d
                INNER JOIN dbo.Usuario u ON d.usuario = u.id
                WHERE d.id = ?
                """,
                id
        );

        final String nuevoEstado = Objects.toString(docenteActualizado.get("estado"), "ACTIVO");
        docenteActualizado.put("mensajeUsuario", "Estado del docente actualizado a " + nuevoEstado + ".");

        return ResponseEntity.ok(new ApiDataResponse<>(true, docenteActualizado));
    }

    @GetMapping("/planes-estudio")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarPlanesEstudio() {
        final UUID programaId = resolveCurrentProgramaId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_PLANES_PROGRAMA, programaId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando planes para programaId={}", programaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/planes-estudio")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearPlanEstudio(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID programaId = resolveCurrentProgramaId();
        final UUID id = UUID.randomUUID();
        final String codigo = Objects.toString(payload.get("codigo"), "PLAN-" + id.toString().substring(0, 6));
        final String nombre = Objects.toString(payload.get("nombre"), "Nuevo Plan");
        final int creditos = ((Number) payload.getOrDefault("totalCreditos", 160)).intValue();
        final int semestres = ((Number) payload.getOrDefault("totalSemestres", 10)).intValue();
        final int anio = ((Number) payload.getOrDefault("anioVigencia", LocalDate.now().getYear())).intValue();

        jdbcTemplate.update(SQL_INSERT_PLAN, id, codigo, nombre, creditos, semestres, anio, programaId);

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", id.toString());
        result.put("programa", "Ingeniería de Sistemas");
        result.put("totalAsignaturas", 0);
        result.put("estado", "VIGENTE");

        realtimeEventHub.broadcast("PLANES", "CREADO", result);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PutMapping("/planes-estudio/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarPlanEstudio(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID programaId = resolveCurrentProgramaId();
        final String codigo = Objects.toString(payload.get("codigo"), "PLAN");
        final String nombre = Objects.toString(payload.get("nombre"), "Plan");
        final int creditos = ((Number) payload.getOrDefault("totalCreditos", 160)).intValue();
        final int semestres = ((Number) payload.getOrDefault("totalSemestres", 10)).intValue();
        final int anio = ((Number) payload.getOrDefault("anioVigencia", LocalDate.now().getYear())).intValue();

        final int updated = jdbcTemplate.update(SQL_UPDATE_PLAN, codigo, nombre, creditos, semestres, anio, id, programaId);
        if (updated == 0) {
            throw new ForbiddenException("El plan no pertenece a su programa académico.");
        }

        final Map<String, Object> updatedData = new HashMap<>(payload);
        updatedData.put("id", id.toString());
        realtimeEventHub.broadcast("PLANES", "ACTUALIZADO", updatedData);

        return ResponseEntity.ok(new ApiDataResponse<>(true, payload));
    }

    @PatchMapping("/planes-estudio/{id}/toggle-estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoPlanEstudio(
            @PathVariable final UUID id
    ) {
        final UUID programaId = resolveCurrentProgramaId();
        final int updated = jdbcTemplate.update(SQL_TOGGLE_ESTADO_PLAN, id, programaId);
        if (updated == 0) {
            throw new ForbiddenException("El plan no pertenece a su programa académico o no existe.");
        }

        final Integer nuevoEstado = jdbcTemplate.queryForObject(
                "SELECT estado FROM dbo.PlanEstudio WHERE id = ?",
                Integer.class,
                id
        );

        final String estadoDesc = nuevoEstado != null && nuevoEstado == 1 ? "VIGENTE" : "INACTIVO";
        final Map<String, Object> result = Map.of(
                "id", id.toString(),
                "estado", estadoDesc,
                "mensajeUsuario", "Estado del plan de estudios actualizado correctamente a: " + estadoDesc
        );

        realtimeEventHub.broadcast("PLANES", "ESTADO_CAMBIADO", result);

        return ResponseEntity.ok(new ApiDataResponse<>(true, result));
    }

    @PostMapping("/planes-estudio/{id}/semestres")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> agregarSemestrePlan(
            @PathVariable final UUID id
    ) {
        final Integer totalSemestres = jdbcTemplate.queryForObject(
                "SELECT totalSemestres FROM dbo.PlanEstudio WHERE id = ?",
                Integer.class,
                id
        );
        if (totalSemestres == null) {
            throw new ResourceNotFoundException("El plan de estudios no existe.");
        }
        final int nuevoSemestreNumero = totalSemestres + 1;

        final UUID semId = jdbcTemplate.query(
                "SELECT TOP 1 id FROM dbo.Semestre WHERE numero = ?",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                nuevoSemestreNumero
        );

        final UUID speId = UUID.randomUUID();
        if (semId != null) {
            jdbcTemplate.update(
                    "IF NOT EXISTS (SELECT 1 FROM dbo.SemestrePlanEstudio WHERE planEstudio = ? AND semestre = ?) " +
                    "BEGIN INSERT INTO dbo.SemestrePlanEstudio (id, planEstudio, semestre) VALUES (?, ?, ?) END",
                    id, semId, speId, id, semId
            );
        }

        jdbcTemplate.update("UPDATE dbo.PlanEstudio SET totalSemestres = ? WHERE id = ?", nuevoSemestreNumero, id);

        final Map<String, Object> res = Map.of(
                "planEstudioId", id.toString(),
                "semestreNumero", nuevoSemestreNumero,
                "totalSemestres", nuevoSemestreNumero,
                "mensajeUsuario", "Nivel semestral " + nuevoSemestreNumero + " añadido exitosamente al plan."
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
    }

    @DeleteMapping("/planes-estudio/{id}/semestres/{numero}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> eliminarSemestrePlan(
            @PathVariable final UUID id,
            @PathVariable final int numero
    ) {
        final Integer asigCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1) FROM dbo.Asignatura a
                INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                INNER JOIN dbo.Semestre s ON spe.semestre = s.id
                WHERE spe.planEstudio = ? AND s.numero = ?
                """,
                Integer.class,
                id, numero
        );

        if (asigCount != null && asigCount > 0) {
            throw new ConflictException("No es posible eliminar el semestre " + numero + " porque contiene " + asigCount + " asignatura(s).");
        }

        jdbcTemplate.update(
                """
                DELETE spe FROM dbo.SemestrePlanEstudio spe
                INNER JOIN dbo.Semestre s ON spe.semestre = s.id
                WHERE spe.planEstudio = ? AND s.numero = ?
                """,
                id, numero
        );

        jdbcTemplate.update("UPDATE dbo.PlanEstudio SET totalSemestres = IIF(totalSemestres > 1, totalSemestres - 1, 1) WHERE id = ?", id);

        final Map<String, Object> res = Map.of(
                "planEstudioId", id.toString(),
                "semestreEliminado", numero,
                "mensajeUsuario", "Semestre " + numero + " eliminado exitosamente del plan de estudios."
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @GetMapping("/planes-estudio/{id}/asignaturas")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarAsignaturasPlan(
            @PathVariable final UUID id
    ) {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_ASIGNATURAS_PLAN, id);

            // Enriquecer cada asignatura con su lista de prerrequisitos (códigos)
            final List<Map<String, Object>> prereqList = jdbcTemplate.queryForList(SQL_CONSULTAR_PRERREQUISITOS_PLAN, id);
            final java.util.Map<String, List<String>> prereqMap = new HashMap<>();
            for (final Map<String, Object> row : prereqList) {
                final String asigId = Objects.toString(row.get("asignaturaId"), "");
                final String cod = Objects.toString(row.get("codigoRequisito"), "");
                prereqMap.computeIfAbsent(asigId, k -> new ArrayList<>()).add(cod);
            }

            final List<Map<String, Object>> enriched = new ArrayList<>();
            for (final Map<String, Object> a : list) {
                final Map<String, Object> m = new HashMap<>(a);
                final String asigId = Objects.toString(m.get("id"), "");
                m.put("prerrequisitos", prereqMap.getOrDefault(asigId, List.of()));
                enriched.add(m);
            }

            return ResponseEntity.ok(new ApiListResponse<>(true, enriched, enriched.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando asignaturas para planId={}", id, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }


    @PostMapping("/planes-estudio/{id}/asignaturas")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearAsignaturaPlan(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = Objects.toString(payload.get("nombre"), "").trim();
        final String codigo = Objects.toString(payload.get("codigo"), "").trim();
        final int creditos = ((Number) payload.getOrDefault("creditos", 3)).intValue();
        final int semestreNumero = ((Number) payload.getOrDefault("semestre", 1)).intValue();
        final String areaNombre = Objects.toString(payload.getOrDefault("area", ""), "").trim();
        final String componenteNombre = Objects.toString(payload.getOrDefault("componente", ""), "").trim();

        if (nombre.isEmpty() || codigo.isEmpty()) {
            throw new ValidationException("El código y nombre de la asignatura son obligatorios.");
        }
        if (creditos <= 0) {
            throw new ValidationException("Los créditos deben ser mayores a cero.");
        }

        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_crear_asignatura @codigo = ?, @nombre = ?, @creditos = ?, @idPlanEstudio = ?, @semestreNumero = ?, @nombreArea = ?, @nombreComponente = ?, @idCorrelacion = ?, @idAsignaturaResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    codigo, nombre, creditos, id, semestreNumero,
                    areaNombre.isEmpty() ? null : areaNombre,
                    componenteNombre.isEmpty() ? null : componenteNombre,
                    correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Asignatura registrada.");
            if (!exitoso) {
                throw new ConflictException(mensajeUsuario);
            }

            final Object idAsigObj = spResult.get("idAsignatura");
            final String idAsig = idAsigObj != null ? idAsigObj.toString() : UUID.randomUUID().toString();

            // Sincronizar prerrequisitos si se enviaron
            if (payload.get("prerrequisitos") instanceof List<?> prerreqs && !prerreqs.isEmpty()) {
                sincronizarPrerrequisitos(UUID.fromString(idAsig), prerreqs.stream().map(Object::toString).toList(), id);
            }

            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", idAsig);
            result.put("planEstudioId", id.toString());
            result.put("mensajeUsuario", mensajeUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));

        } catch (ConflictException | ValidationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_crear_asignatura para planId={}", id, ex);
            throw new ConflictException("No fue posible registrar la asignatura. " + ex.getMostSpecificCause().getMessage());
        }
    }

    @PutMapping("/planes-estudio/{planId}/asignaturas/{asigId}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarAsignaturaPlan(
            @PathVariable final UUID planId,
            @PathVariable final UUID asigId,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = Objects.toString(payload.get("nombre"), "").trim();
        final String codigo = Objects.toString(payload.get("codigo"), "").trim();
        final int creditos = ((Number) payload.getOrDefault("creditos", 3)).intValue();
        final int semestreNumero = ((Number) payload.getOrDefault("semestre", 1)).intValue();
        final String areaNombre = Objects.toString(payload.getOrDefault("area", ""), "").trim();

        if (nombre.isEmpty() || codigo.isEmpty()) {
            throw new ValidationException("El código y nombre de la asignatura son obligatorios.");
        }

        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_actualizar_asignatura @id = ?, @codigo = ?, @nombre = ?, @creditos = ?, @idPlanEstudio = ?, @semestreNumero = ?, @nombreArea = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    asigId, codigo, nombre, creditos, planId, semestreNumero,
                    areaNombre.isEmpty() ? null : areaNombre,
                    correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Asignatura actualizada.");
            if (!exitoso) {
                throw new ResourceNotFoundException(mensajeUsuario);
            }

            // Sincronizar prerrequisitos si se enviaron
            if (payload.get("prerrequisitos") instanceof List<?> prerreqs) {
                sincronizarPrerrequisitos(asigId, prerreqs.stream().map(Object::toString).toList(), planId);
            }

            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", asigId.toString());
            result.put("mensajeUsuario", mensajeUsuario);
            return ResponseEntity.ok(new ApiDataResponse<>(true, result));

        } catch (ResourceNotFoundException | ValidationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_actualizar_asignatura asigId={}", asigId, ex);
            throw new ConflictException("No fue posible actualizar la asignatura. " + ex.getMostSpecificCause().getMessage());
        }
    }

    @DeleteMapping("/planes-estudio/{planId}/asignaturas/{asigId}")
    public ResponseEntity<ApiDataResponse<Boolean>> eliminarAsignaturaPlan(
            @PathVariable final UUID planId,
            @PathVariable final UUID asigId
    ) {
        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_eliminar_asignatura @id = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    asigId, correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Asignatura eliminada.");
            if (!exitoso) {
                throw new ConflictException(mensajeUsuario);
            }
            return ResponseEntity.ok(new ApiDataResponse<>(true, true));

        } catch (ConflictException | ResourceNotFoundException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_eliminar_asignatura asigId={}", asigId, ex);
            throw new ConflictException("No fue posible eliminar la asignatura. " + ex.getMostSpecificCause().getMessage());
        }
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> listarTodasAsignaturas() {
        final UUID programaId = resolveCurrentProgramaId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                    SELECT
                        a.id,
                        spe.planEstudio AS planEstudioId,
                        a.codigo,
                        a.nombre,
                        a.credito AS creditos,
                        s.numero AS semestre,
                        ISNULL(ar.nombre, 'Ciencias Computacionales') AS area,
                        ISNULL(c.nombre, 'Obligatoria') AS componente,
                        4 AS horasSemanales,
                        IIF(a.estado = 1, 'ACTIVO', 'INACTIVO') AS estado
                    FROM dbo.Asignatura a
                    INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                    INNER JOIN dbo.Semestre s ON spe.semestre = s.id
                    INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
                    LEFT JOIN dbo.Area ar ON a.area = ar.id
                    LEFT JOIN dbo.Componente c ON a.componente = c.id
                    WHERE pe.programa = ?
                    ORDER BY s.numero, a.nombre
                    """, programaId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error listando asignaturas del programa {}", programaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PatchMapping("/asignaturas/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoAsignatura(
            @PathVariable final UUID id
    ) {
        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_toggle_estado_asignatura @id = ?, @idCorrelacion = ?, @nuevoEstado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Estado actualizado.");
            if (!exitoso) {
                throw new ResourceNotFoundException(mensajeUsuario);
            }

            final Object estadoObj = spResult.get("estadoActualizado");
            final String estadoDesc = estadoObj != null
                    && (Boolean.TRUE.equals(estadoObj) || Integer.valueOf(1).equals(estadoObj) || "1".equals(estadoObj.toString()))
                    ? "ACTIVO" : "INACTIVO";
            final Map<String, Object> result = Map.of(
                    "id", id.toString(),
                    "estado", estadoDesc,
                    "mensajeUsuario", mensajeUsuario
            );
            return ResponseEntity.ok(new ApiDataResponse<>(true, result));

        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_toggle_estado_asignatura id={}", id, ex);
            throw new ConflictException("No fue posible cambiar el estado de la asignatura.");
        }
    }

    /**
     * Sincroniza los prerrequisitos de una asignatura eliminando los existentes e insertando los nuevos por código.
     */
    private void sincronizarPrerrequisitos(final UUID asigId, final List<String> codigos, final UUID planId) {
        jdbcTemplate.update("DELETE FROM dbo.PrerrequisitoAsignatura WHERE asignatura = ?", asigId);
        for (final String codigoRaw : codigos) {
            final String codigo = codigoRaw.trim().toUpperCase();
            if (codigo.isEmpty()) {
                continue;
            }
            try {
                final UUID reqId = jdbcTemplate.query(
                        """
                        SELECT TOP 1 a.id FROM dbo.Asignatura a
                        INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                        WHERE spe.planEstudio = ? AND UPPER(a.codigo) = ?
                        """,
                        rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                        planId, codigo
                );
                if (reqId != null) {
                    jdbcTemplate.update(
                            "INSERT INTO dbo.PrerrequisitoAsignatura (id, asignatura, asignaturaRequisito, tipo) VALUES (NEWID(), ?, ?, 'OBLIGATORIO')",
                            asigId, reqId
                    );
                } else {
                    LOGGER.warn("No se encontró asignatura prereq con código {} en plan {}", codigo, planId);
                }
            } catch (DataAccessException ex) {
                LOGGER.warn("Error insertando prerrequisito código={} para asig={}: {}", codigo, asigId, ex.getMessage());
            }
        }
    }



    @GetMapping("/periodos-academicos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarPeriodosAcademicos() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_PERIODOS);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando períodos académicos", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/periodos-academicos")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearPeriodoAcademico(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String nombre = Objects.toString(payload.get("nombre"), "Periodo Académico");
        final String rawCodigo = Objects.toString(payload.get("codigo"), "20261").replaceAll("[^0-9]", "");
        final int codigo = rawCodigo.isEmpty() ? 20261 : Integer.parseInt(rawCodigo);
        final String fechaInicio = Objects.toString(payload.get("fechaInicio"), "2026-08-01");
        final String fechaFin = Objects.toString(payload.get("fechaFin"), "2026-12-15");
        final int anio = LocalDate.parse(fechaInicio).getYear();

        final UUID institucionId = jdbcTemplate.query(
                "SELECT TOP 1 id FROM dbo.Institucion",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("B1C2D3E4-0000-0000-0000-000000000001")
        );

        jdbcTemplate.update(
                "INSERT INTO dbo.PeriodoAcademico (id, institucion, nombre, codigo, fechaInicio, fechaFin, anio) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, institucionId, nombre, codigo, fechaInicio, fechaFin, anio
        );

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", id.toString());
        result.put("nombre", nombre);
        result.put("codigo", codigo);
        result.put("fechaInicio", fechaInicio);
        result.put("fechaFin", fechaFin);
        result.put("estado", "ACTIVO");
        result.put("mensajeUsuario", "Período académico " + nombre + " registrado exitosamente.");

        realtimeEventHub.broadcast("PERIODOS", "CREADO", result);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PutMapping("/periodos-academicos/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarPeriodoAcademico(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = payload.containsKey("nombre") ? Objects.toString(payload.get("nombre"), null) : null;
        final String fechaInicio = payload.containsKey("fechaInicio") ? Objects.toString(payload.get("fechaInicio"), null) : null;
        final String fechaFin = payload.containsKey("fechaFin") ? Objects.toString(payload.get("fechaFin"), null) : null;

        Integer codigo = null;
        if (payload.get("codigo") != null) {
            final String raw = payload.get("codigo").toString().replaceAll("[^0-9]", "");
            if (!raw.isEmpty()) codigo = Integer.parseInt(raw);
        }

        Boolean estado = null;
        if (payload.containsKey("estado") && payload.get("estado") != null) {
            final String est = payload.get("estado").toString().trim().toUpperCase();
            // ACTIVO -> 1; CERRADO / INACTIVO -> 0; PLANEACION -> 1
            if ("ACTIVO".equals(est) || "PLANEACION".equals(est) || "TRUE".equals(est) || "1".equals(est)) {
                estado = true;
            } else {
                estado = false;
            }
        }

        final int updated = jdbcTemplate.update(
                """
                UPDATE dbo.PeriodoAcademico
                SET nombre = ISNULL(?, nombre),
                    codigo = ISNULL(?, codigo),
                    fechaInicio = ISNULL(?, fechaInicio),
                    fechaFin = ISNULL(?, fechaFin),
                    anio = IIF(? IS NOT NULL, YEAR(CAST(? AS DATE)), anio),
                    estado = ISNULL(?, estado)
                WHERE id = ?
                """,
                nombre, codigo, fechaInicio, fechaFin, fechaInicio, fechaInicio, estado, id
        );

        if (updated == 0) {
            throw new ResourceNotFoundException("El período académico especificado no existe.");
        }

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("mensajeUsuario", "Período académico actualizado exitosamente.");

        realtimeEventHub.broadcast("PERIODOS", "ACTUALIZADO", res);

        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @PatchMapping("/periodos-academicos/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoPeriodoAcademico(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                "UPDATE dbo.PeriodoAcademico SET estado = IIF(estado = 1, 0, 1) WHERE id = ?",
                id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("El período académico especificado no existe.");
        }

        final Integer estadoActual = jdbcTemplate.queryForObject(
                "SELECT estado FROM dbo.PeriodoAcademico WHERE id = ?",
                Integer.class,
                id
        );
        final String desc = estadoActual != null && estadoActual == 1 ? "ACTIVO" : "INACTIVO";

        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", desc,
                "mensajeUsuario", "Estado del período académico modificado a: " + desc
        );

        realtimeEventHub.broadcast("PERIODOS", "ESTADO_CAMBIADO", res);

        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @GetMapping("/estudiantes")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarEstudiantesPrograma() {
        final UUID programaId = resolveCurrentProgramaId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_ESTUDIANTES_PROGRAMA, programaId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando estudiantes para programaId={}", programaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @GetMapping("/solicitudes-matricula")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarSolicitudesMatricula() {
        final UUID programaId = resolveCurrentProgramaId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_SOLICITUDES_MATRICULA, programaId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando solicitudes de matrícula para programaId={}", programaId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PatchMapping("/solicitudes-matricula/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> resolverSolicitudMatricula(
            @PathVariable final UUID id,
            @RequestBody final Map<String, String> payload
    ) {
        final UUID coordinadorId = resolveCurrentCoordinadorId();
        final String accion = payload.getOrDefault("accion", "RECHAZADA").toUpperCase();
        final String respuesta = payload.getOrDefault("respuesta", "");
        final UUID correlacion = UUID.randomUUID();

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_resolver_solicitud_matricula @idSolicitud = ?, @idCoordinador = ?, @accion = ?, @respuestaCoordinador = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, coordinadorId, accion, respuesta, correlacion
            );

            final Boolean exitoso = (Boolean) spResult.getOrDefault("exitoso", true);
            if (Boolean.FALSE.equals(exitoso)) {
                final String mensaje = (String) spResult.getOrDefault("mensajeUsuario", "Acceso denegado o error al resolver solicitud.");
                if (mensaje.contains("Acceso denegado")) {
                    throw new ForbiddenException(mensaje);
                }
                throw new ResourceNotFoundException(mensaje);
            }

            final Map<String, Object> res = Map.of(
                    "id", id.toString(),
                    "estado", accion,
                    "respuestaCoordinador", respuesta
            );
            return ResponseEntity.ok(new ApiDataResponse<>(true, res));
        } catch (ForbiddenException | ResourceNotFoundException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al ejecutar usp_resolver_solicitud_matricula", ex);
            jdbcTemplate.update(SQL_RESOLVER_SOLICITUD_MATRICULA, accion, respuesta, id);

            if ("APROBADA".equals(accion)) {
                jdbcTemplate.query(
                        "SELECT estudiante, grupo FROM dbo.SolicitudMatricula WHERE id = ?",
                        rs -> {
                            if (rs.next()) {
                                final UUID estudiante = UUID.fromString(rs.getString("estudiante"));
                                final UUID grupo = UUID.fromString(rs.getString("grupo"));
                                jdbcTemplate.update(
                                        "IF NOT EXISTS (SELECT 1 FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?) " +
                                        "BEGIN INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado) VALUES (NEWID(), ?, ?, (SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A')) END",
                                        estudiante, grupo, estudiante, grupo
                                );
                            }
                            return null;
                        },
                        id
                );
            }

            final Map<String, Object> res = Map.of(
                    "id", id.toString(),
                    "estado", accion,
                    "respuestaCoordinador", respuesta
            );
            return ResponseEntity.ok(new ApiDataResponse<>(true, res));
        }
    }

    @PostMapping("/grupos/{grupoId}/estudiantes")
    public ResponseEntity<ApiDataResponse<Boolean>> matricularEstudianteEnGrupo(
            @PathVariable final UUID grupoId,
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID estudianteId = UUID.fromString(payload.get("estudianteId").toString());
        final UUID correlacion = UUID.randomUUID();

        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                Integer.class, estudianteId, grupoId
        );
        if (count != null && count > 0) {
            throw new ConflictException("El estudiante ya se encuentra matriculado en este grupo académico.");
        }

        try {
            jdbcTemplate.update(
                    "EXEC dbo.usp_registrar_estudiante_en_grupo_interno @idEstudiante = ?, @idGrupo = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    estudianteId, grupoId, correlacion
            );
        } catch (DataAccessException ex) {
            LOGGER.warn("Fallo SP usp_registrar_estudiante_en_grupo_interno, ejecutando fallback directo SQL: {}", ex.getMessage());
            jdbcTemplate.update(SQL_ENROLL_ESTUDIANTE_GRUPO, estudianteId, grupoId);
        }

        return ResponseEntity.ok(new ApiDataResponse<>(true, true));
    }

    @DeleteMapping("/grupos/{grupoId}/estudiantes/{estudianteId}")
    public ResponseEntity<ApiDataResponse<Boolean>> retirarEstudianteDeGrupo(
            @PathVariable final UUID grupoId,
            @PathVariable final UUID estudianteId
    ) {
        jdbcTemplate.update(SQL_RETIRAR_ESTUDIANTE_GRUPO, estudianteId, grupoId);
        return ResponseEntity.ok(new ApiDataResponse<>(true, true));
    }

    private UUID resolveCurrentProgramaId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000002"));

        return userScopeService.findProgramaIdByCoordinadorUsuario(usuarioId)
                .orElse(UUID.fromString("B2C3D4E5-0000-0000-0000-000000000001"));
    }

    private UUID resolveCurrentCoordinadorId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000002"));

        return userScopeService.findCoordinadorIdByUsuario(usuarioId)
                .orElse(UUID.fromString("D1E2F3A4-0000-0000-0000-000000000002"));
    }
}
