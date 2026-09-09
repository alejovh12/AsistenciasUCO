package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper.RegistrarEstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.validation.RegistrarEstudianteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Controlador REST para operaciones sobre Grupos Académicos (HU043, HU044, HU050-HU052).
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoController.class);

    static final String SQL_LISTAR_GRUPOS_BASE = """
            SELECT
                g.id,
                g.codigo,
                CONCAT('GRP-', g.codigo) AS codigoGrupo,
                g.nombre,
                g.aula,
                g.cantidadEstudiantes AS cupoMaximo,
                (SELECT COUNT(1) FROM dbo.EstudianteGrupo eg WHERE eg.grupo = g.id) AS estudiantesActivos,
                (SELECT COUNT(1) FROM dbo.EstudianteGrupo eg WHERE eg.grupo = g.id) AS totalEstudiantes,
                a.id AS asignaturaId,
                a.codigo AS codigoAsignatura,
                a.nombre AS nombreAsignatura,
                a.nombre AS nombreMateria,
                d.id AS docenteId,
                CONCAT(u.primerNombre, ' ', ISNULL(u.segundoNombre + ' ', ''), u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS nombreDocente,
                g.periodoAcademico AS periodoAcademicoId,
                'ACTIVO' AS estado,
                'Lun, Mié 08:00 - 10:00 AM' AS horario
            FROM dbo.Grupo g
            INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
            INNER JOIN dbo.Docente d ON g.docente = d.id
            INNER JOIN dbo.Usuario u ON d.usuario = u.id
            """;

    static final String SQL_LISTAR_ESTUDIANTES_GRUPO = """
            SELECT
                e.id,
                CAST(u.numeroIdentificacion AS VARCHAR(20)) AS documento,
                CAST(u.numeroIdentificacion AS VARCHAR(20)) AS codigo,
                CONCAT(u.primerNombre, ' ', ISNULL(u.segundoNombre + ' ', ''), u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS nombreCompleto,
                u.correo,
                CONVERT(VARCHAR(10), CURRENT_TIMESTAMP, 120) AS fechaInscripcion,
                'ACTIVO' AS estado
            FROM dbo.EstudianteGrupo eg
            INNER JOIN dbo.Estudiante e ON eg.estudiante = e.id
            INNER JOIN dbo.Usuario u ON e.usuario = u.id
            WHERE eg.grupo = ?
            ORDER BY u.primerApellido, u.primerNombre
            """;

    private static final RegistrarEstudianteRequestValidator REQUEST_VALIDATOR = new RegistrarEstudianteRequestValidator();

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;
    private final RegistrarEstudianteInputPort registrarEstudianteInputPort;
    private final ConsultarGruposInputPort consultarGruposInputPort;
    private final IdentityProviderPort identityProviderPort;

    @org.springframework.beans.factory.annotation.Autowired
    public GrupoController(
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            final RegistrarEstudianteInputPort registrarEstudianteInputPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            final ConsultarGruposInputPort consultarGruposInputPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            final IdentityProviderPort identityProviderPort
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userScopeService = userScopeService;
        this.registrarEstudianteInputPort = registrarEstudianteInputPort;
        this.consultarGruposInputPort = consultarGruposInputPort;
        this.identityProviderPort = identityProviderPort;
    }

    public GrupoController(
            final RegistrarEstudianteInputPort registrarEstudianteInputPort,
            final ConsultarGruposInputPort consultarGruposInputPort
    ) {
        this(null, null, registrarEstudianteInputPort, consultarGruposInputPort, null);
    }

    @GetMapping
    public ResponseEntity<?> listarGrupos() {
        if (jdbcTemplate == null && consultarGruposInputPort != null) {
            return ResponseEntity.ok(consultarGruposInputPort.execute());
        }
        final Optional<UUID> authUserId = userScopeService != null
                ? userScopeService.getAuthenticatedUserId()
                : Optional.empty();
        try {
            List<Map<String, Object>> list;
            if (authUserId.isPresent()) {
                final Optional<UUID> docenteId = userScopeService.findDocenteIdByUsuario(authUserId.get());
                if (docenteId.isPresent()) {
                    list = jdbcTemplate.queryForList(SQL_LISTAR_GRUPOS_BASE + " WHERE g.docente = ? ORDER BY g.nombre", docenteId.get());
                } else {
                    final Optional<UUID> progId = userScopeService.findProgramaIdByCoordinadorUsuario(authUserId.get());
                    if (progId.isPresent()) {
                        list = jdbcTemplate.queryForList(
                                SQL_LISTAR_GRUPOS_BASE +
                                " INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id " +
                                " INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id " +
                                " WHERE pe.programa = ? ORDER BY g.nombre",
                                progId.get()
                        );
                    } else {
                        list = jdbcTemplate.queryForList(SQL_LISTAR_GRUPOS_BASE + " ORDER BY g.nombre");
                    }
                }
            } else {
                list = jdbcTemplate.queryForList(SQL_LISTAR_GRUPOS_BASE + " ORDER BY g.nombre");
            }
            list.forEach(g -> g.putIfAbsent("grupoHabilitado", true));
            return ResponseEntity.ok(list);
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando grupos académicos", ex);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearGrupo(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String nombre = Objects.toString(payload.get("name"), Objects.toString(payload.get("nombre"), "Grupo 01")).trim();
        final String rawCodigo = Objects.toString(payload.get("code"), Objects.toString(payload.get("codigo"), "101")).replaceAll("[^0-9]", "");
        final int codigo = rawCodigo.isEmpty() ? ((int) (System.currentTimeMillis() % 9000) + 1000) : Integer.parseInt(rawCodigo);
        final int cupo = ((Number) payload.getOrDefault("cupoMaximo", 35)).intValue();
        final String aula = Objects.toString(payload.get("room"), Objects.toString(payload.get("aula"), "Aula Por Asignar")).trim();

        // Resolver Asignatura
        UUID asigId = null;
        if (payload.get("asignaturaId") != null) {
            asigId = UUID.fromString(payload.get("asignaturaId").toString());
        } else if (payload.get("code") != null) {
            final String codAsig = payload.get("code").toString();
            asigId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Asignatura WHERE LOWER(codigo) = LOWER(?)",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    codAsig
            );
        }
        if (asigId == null) {
            asigId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Asignatura",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("E2F3A4B5-0000-0000-0000-000000000001")
            );
        }

        // Resolver Docente
        UUID docenteId = null;
        if (payload.get("docenteId") != null) {
            docenteId = UUID.fromString(payload.get("docenteId").toString());
        } else {
            final Optional<UUID> authUserId = userScopeService.getAuthenticatedUserId();
            if (authUserId.isPresent()) {
                docenteId = userScopeService.findDocenteIdByUsuario(authUserId.get()).orElse(null);
            }
        }
        if (docenteId == null) {
            docenteId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Docente",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("F1A2B3C4-0000-0000-0000-000000000003")
            );
        }

        // Resolver Periodo
        UUID periodoId = null;
        if (payload.get("periodoAcademicoId") != null) {
            periodoId = UUID.fromString(payload.get("periodoAcademicoId").toString());
        } else {
            periodoId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.PeriodoAcademico ORDER BY fechaInicio DESC",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
            );
        }

        // 1. Validar que la asignatura pertenezca a la oferta habilitada/asignada para el docente
        final Integer asignada = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM dbo.Asignatura a
                WHERE a.id = ?
                AND (
                    EXISTS (SELECT 1 FROM dbo.Grupo g WHERE g.docente = ? AND g.asignatura = a.id)
                    OR EXISTS (
                        SELECT 1
                        FROM dbo.SemestrePlanEstudio spe
                        INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
                        WHERE a.semestrePlanEstudio = spe.id
                        AND pe.programa IN (
                            SELECT pe2.programa
                            FROM dbo.Grupo g2
                            INNER JOIN dbo.Asignatura a2 ON g2.asignatura = a2.id
                            INNER JOIN dbo.SemestrePlanEstudio spe2 ON a2.semestrePlanEstudio = spe2.id
                            INNER JOIN dbo.PlanEstudio pe2 ON spe2.planEstudio = pe2.id
                            WHERE g2.docente = ?
                        )
                    )
                )
                """, Integer.class, asigId, docenteId, docenteId);

        if (asignada == null || asignada == 0) {
            throw new ConflictException("La asignatura seleccionada no se encuentra asignada al docente titular.");
        }

        // 2. Validar horario y detectar colisiones con otros grupos del docente
        final List<UUID> diasIds = resolverDiasIds(payload);
        final String horaInicioStr = extraerHora(payload, "horaInicio", "08:00");
        final String horaFinStr = extraerHora(payload, "horaFin", "10:00");

        for (final UUID diaId : diasIds) {
            final List<Map<String, Object>> conflictos = jdbcTemplate.queryForList("""
                    SELECT TOP 1 g.nombre AS grupoNombre, d.nombre AS diaNombre,
                           CONVERT(VARCHAR(5), h.horaInicio, 108) AS inicio,
                           CONVERT(VARCHAR(5), h.horaFin, 108) AS fin
                    FROM dbo.Horario h
                    INNER JOIN dbo.Grupo g ON h.grupo = g.id
                    INNER JOIN dbo.Dia d ON h.dia = d.id
                    WHERE g.docente = ?
                      AND h.dia = ?
                      AND g.periodoAcademico = ?
                      AND (? < h.horaFin AND ? > h.horaInicio)
                    """, docenteId, diaId, periodoId, horaInicioStr, horaFinStr);

            if (!conflictos.isEmpty()) {
                final Map<String, Object> c = conflictos.get(0);
                throw new ConflictException(String.format(
                        "Cruce de horario detectado: El docente ya tiene clases asignadas el día %s de %s a %s en el grupo '%s'.",
                        c.get("diaNombre"), c.get("inicio"), c.get("fin"), c.get("grupoNombre")
                ));
            }
        }

        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_crear_grupo @id = ?, @idAsignatura = ?, @idPeriodoAcademico = ?, @codigo = ?, @nombre = ?, @idDocente = ?, @cupoMaximo = ?, @aula = ?, @idCorrelacion = ?, @idGrupoResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, asigId, periodoId, codigo, nombre, docenteId, cupo, aula, correlacion
            );

            final boolean exitoso = spResult.get("estadoResultado") instanceof Boolean b ? b
                    : (spResult.get("exitoso") instanceof Boolean b2 ? b2
                    : ((Number) spResult.getOrDefault("estadoResultado", spResult.getOrDefault("exitoso", 1))).intValue() == 1);
            final String mensajeUsuario = Objects.toString(
                    spResult.getOrDefault("mensajeUsuarioResultado", spResult.get("mensajeUsuario")),
                    "Grupo creado exitosamente."
            );
            if (!exitoso) {
                if (mensajeUsuario.contains("Ya existe") || mensajeUsuario.contains("Conflicto")) {
                    throw new ConflictException(mensajeUsuario);
                }
                throw new ValidationException(mensajeUsuario);
            }

            // 3. Persistir franjas horarias en dbo.Horario
            for (final UUID diaId : diasIds) {
                jdbcTemplate.update(
                        "INSERT INTO dbo.Horario (id, grupo, dia, horaInicio, horaFin) VALUES (NEWID(), ?, ?, CAST(? AS TIME), CAST(? AS TIME))",
                        id, diaId, horaInicioStr, horaFinStr
                );
            }

            // 4. Generación opcional automática de sesiones del semestre según el horario
            final boolean generarSesiones = payload.containsKey("generarSesionesAutomaticas")
                    ? Boolean.parseBoolean(Objects.toString(payload.get("generarSesionesAutomaticas"), "true"))
                    : (payload.containsKey("crearSesionesAutomaticamente")
                    ? Boolean.parseBoolean(Objects.toString(payload.get("crearSesionesAutomaticamente"), "true"))
                    : true);

            int sesionesGeneradas = 0;
            if (generarSesiones) {
                try {
                    final Map<String, Object> periodoFechas = jdbcTemplate.queryForMap(
                            "SELECT fechaInicio, fechaFin FROM dbo.PeriodoAcademico WHERE id = ?",
                            periodoId
                    );
                    final LocalDate inicio = periodoFechas.get("fechaInicio") instanceof java.sql.Date d ? d.toLocalDate()
                            : (periodoFechas.get("fechaInicio") instanceof java.sql.Timestamp ts ? ts.toLocalDateTime().toLocalDate()
                            : LocalDate.now());
                    final LocalDate fin = periodoFechas.get("fechaFin") instanceof java.sql.Date d ? d.toLocalDate()
                            : (periodoFechas.get("fechaFin") instanceof java.sql.Timestamp ts ? ts.toLocalDateTime().toLocalDate()
                            : inicio.plusWeeks(16));

                    final Set<Integer> diasSemanaNumeros = new HashSet<>();
                    for (final UUID diaId : diasIds) {
                        final String str = diaId.toString();
                        if (str.endsWith("0001")) diasSemanaNumeros.add(1);
                        else if (str.endsWith("0002")) diasSemanaNumeros.add(2);
                        else if (str.endsWith("0003")) diasSemanaNumeros.add(3);
                        else if (str.endsWith("0004")) diasSemanaNumeros.add(4);
                        else if (str.endsWith("0005")) diasSemanaNumeros.add(5);
                        else if (str.endsWith("0006")) diasSemanaNumeros.add(6);
                        else if (str.endsWith("0007")) diasSemanaNumeros.add(7);
                    }
                    if (diasSemanaNumeros.isEmpty()) {
                        diasSemanaNumeros.add(1);
                        diasSemanaNumeros.add(3);
                    }

                    final LocalTime hInicio = LocalTime.parse(horaInicioStr.length() == 5 ? horaInicioStr : horaInicioStr.substring(0, 5));
                    final LocalTime hFin = LocalTime.parse(horaFinStr.length() == 5 ? horaFinStr : horaFinStr.substring(0, 5));

                    int numeroSesion = 1;
                    LocalDate cur = inicio;
                    while (!cur.isAfter(fin)) {
                        if (diasSemanaNumeros.contains(cur.getDayOfWeek().getValue())) {
                            final UUID sesionId = UUID.randomUUID();
                            final String codigoSesion = "SES-" + String.format("%02d", numeroSesion);
                            final String nombreSesion = "Sesión #" + numeroSesion;
                            final LocalDateTime inicioSesion = cur.atTime(hInicio);
                            final LocalDateTime finSesion = cur.atTime(hFin);
                            final int numeroSemana = ((numeroSesion - 1) / diasSemanaNumeros.size()) + 1;

                            jdbcTemplate.update("""
                                    INSERT INTO dbo.Sesion (id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio, fechaHoraFin, aula, tipo, descripcion, cerrada)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'REGULAR', 'Sesión lectiva del cronograma semestral', 0);
                                    """,
                                    sesionId, nombreSesion, numeroSesion, codigoSesion, numeroSemana, id, inicioSesion, finSesion, aula
                            );
                            numeroSesion++;
                            sesionesGeneradas++;
                        }
                        cur = cur.plusDays(1);
                    }
                } catch (Exception ex) {
                    LOGGER.warn("No fue posible generar las sesiones automáticas para el grupo id={}", id, ex);
                }
            }

            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", id.toString());
            result.put("codigo", codigo);
            result.put("nombre", nombre);
            result.put("aula", aula);
            result.put("cupoMaximo", cupo);
            result.put("docenteId", docenteId.toString());
            result.put("asignaturaId", asigId.toString());
            result.put("sesionesGeneradas", sesionesGeneradas);
            result.put("mensajeUsuario", mensajeUsuario + (sesionesGeneradas > 0 ? " Se generaron " + sesionesGeneradas + " sesiones automáticamente." : ""));

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
        } catch (ConflictException | ValidationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_crear_grupo", ex);
            throw new ConflictException("Error al registrar grupo: " + ex.getMostSpecificCause().getMessage());
        }
    }

    private List<UUID> resolverDiasIds(final Map<String, Object> payload) {
        final List<UUID> dias = new ArrayList<>();
        final Object diasObj = payload.get("dias");
        final String schedule = Objects.toString(payload.get("schedule"), "");

        if (diasObj instanceof List<?> list) {
            for (final Object item : list) {
                final UUID diaId = mapearNombreADiaId(Objects.toString(item, ""));
                if (diaId != null && !dias.contains(diaId)) {
                    dias.add(diaId);
                }
            }
        } else if (diasObj != null && !diasObj.toString().isBlank()) {
            final String[] parts = diasObj.toString().split("[,;|]");
            for (final String p : parts) {
                final UUID diaId = mapearNombreADiaId(p.trim());
                if (diaId != null && !dias.contains(diaId)) {
                    dias.add(diaId);
                }
            }
        }

        if (dias.isEmpty() && !schedule.isBlank()) {
            final String lower = schedule.toLowerCase();
            if (lower.contains("lun")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000001"));
            if (lower.contains("mar")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000002"));
            if (lower.contains("mié") || lower.contains("mie")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000003"));
            if (lower.contains("jue")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000004"));
            if (lower.contains("vie")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000005"));
            if (lower.contains("sáb") || lower.contains("sab")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000006"));
            if (lower.contains("dom")) dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000007"));
        }

        if (dias.isEmpty()) {
            dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000001"));
            dias.add(UUID.fromString("B1B2C3D4-0000-0000-0000-000000000003"));
        }
        return dias;
    }

    private UUID mapearNombreADiaId(final String nombre) {
        final String n = nombre.trim().toUpperCase();
        if (n.startsWith("LUN") || n.equals("LU")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000001");
        if (n.startsWith("MAR") || n.equals("MA")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000002");
        if (n.startsWith("MIE") || n.startsWith("MIÉ") || n.equals("MI")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000003");
        if (n.startsWith("JUE") || n.equals("JU")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000004");
        if (n.startsWith("VIE") || n.equals("VI")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000005");
        if (n.startsWith("SAB") || n.startsWith("SÁB") || n.equals("SA")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000006");
        if (n.startsWith("DOM") || n.equals("DO")) return UUID.fromString("B1B2C3D4-0000-0000-0000-000000000007");
        try {
            return UUID.fromString(nombre);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerHora(final Map<String, Object> payload, final String campo, final String defaultHora) {
        if (payload.containsKey(campo) && payload.get(campo) != null) {
            final String val = payload.get(campo).toString().trim();
            if (!val.isBlank()) {
                return val.length() == 5 ? val + ":00" : val;
            }
        }
        final String schedule = Objects.toString(payload.get("schedule"), "");
        if (!schedule.isBlank()) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})").matcher(schedule);
            if (m.find()) {
                final String h = "horaInicio".equalsIgnoreCase(campo) ? m.group(1) : m.group(2);
                final String pad = h.length() == 4 ? "0" + h : h;
                return pad + ":00";
            }
        }
        return defaultHora + ":00";
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarGrupo(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = payload.containsKey("name") ? payload.get("name").toString()
                : (payload.containsKey("nombre") ? payload.get("nombre").toString() : null);

        Integer codigo = null;
        if (payload.get("code") != null || payload.get("codigo") != null) {
            final String raw = Objects.toString(payload.getOrDefault("code", payload.get("codigo")), "").replaceAll("[^0-9]", "");
            if (!raw.isEmpty()) codigo = Integer.parseInt(raw);
        }

        final Integer cupo = payload.containsKey("cupoMaximo") ? ((Number) payload.get("cupoMaximo")).intValue() : null;
        final String aula = payload.containsKey("room") ? payload.get("room").toString()
                : (payload.containsKey("aula") ? payload.get("aula").toString() : null);

        UUID docenteId = null;
        if (payload.get("docenteId") != null) {
            docenteId = UUID.fromString(payload.get("docenteId").toString());
        }

        final UUID correlacion = UUID.randomUUID();
        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_actualizar_grupo @id = ?, @codigo = ?, @nombre = ?, @idDocente = ?, @cupoMaximo = ?, @aula = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, codigo, nombre, docenteId, cupo, aula, correlacion
            );

            final boolean exitoso = spResult.get("estadoResultado") instanceof Boolean b ? b
                    : (spResult.get("exitoso") instanceof Boolean b2 ? b2
                    : ((Number) spResult.getOrDefault("estadoResultado", spResult.getOrDefault("exitoso", 1))).intValue() == 1);
            final String mensajeUsuario = Objects.toString(
                    spResult.getOrDefault("mensajeUsuarioResultado", spResult.get("mensajeUsuario")),
                    "Grupo actualizado exitosamente."
            );
            if (!exitoso) {
                if (mensajeUsuario.contains("no existe")) throw new ResourceNotFoundException(mensajeUsuario);
                if (mensajeUsuario.contains("ya está en uso")) throw new ConflictException(mensajeUsuario);
                throw new ValidationException(mensajeUsuario);
            }

            final Map<String, Object> result = new HashMap<>(payload);
            result.put("id", id.toString());
            result.put("mensajeUsuario", mensajeUsuario);

            return ResponseEntity.ok(new ApiDataResponse<>(true, result));
        } catch (ConflictException | ResourceNotFoundException | ValidationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_actualizar_grupo", ex);
            throw new ConflictException("Error al actualizar grupo: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @GetMapping("/{grupoId}/estudiantes")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> listarEstudiantesGrupo(
            @PathVariable final UUID grupoId
    ) {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_LISTAR_ESTUDIANTES_GRUPO, grupoId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando estudiantes del grupo {}", grupoId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/{grupoId}/estudiantes")
    public ResponseEntity<RegistrarEstudianteResultadoDTO> matricularEstudiante(
            @PathVariable final UUID grupoId,
            @RequestBody final RegistrarEstudianteRequest request
    ) {
        RequestValidationGuard.validate(REQUEST_VALIDATOR.validate(grupoId, request));

        // Caso 1: Estudiante identificado por ID directo (ej. Coordinador o selector)
        if (request.getEstudianteId() != null) {
            final UUID estudianteId = request.getEstudianteId();
            if (jdbcTemplate != null) {
                final Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                        Integer.class, estudianteId, grupoId
                );
                if (count != null && count > 0) {
                    throw new ConflictException("El estudiante ya se encuentra matriculado en este grupo académico.");
                }
                final UUID estadoActivoId = jdbcTemplate.query(
                        "SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'",
                        rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
                );
                jdbcTemplate.update(
                        "INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado) VALUES (NEWID(), ?, ?, ?)",
                        estudianteId, grupoId, estadoActivoId
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegistrarEstudianteResultadoDTO(true, "Estudiante matriculado exitosamente en el grupo."));
        }

        // Caso 2: Registro completo con datos personales del estudiante
        if (jdbcTemplate != null && request.getNumeroIdentificacion() != null) {
            final UUID existingEstudianteId = jdbcTemplate.query(
                    "SELECT TOP 1 e.id FROM dbo.Estudiante e INNER JOIN dbo.Usuario u ON e.usuario = u.id WHERE u.numeroIdentificacion = ? OR u.correo = ?",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    request.getNumeroIdentificacion(), request.getCorreo()
            );
            if (existingEstudianteId != null) {
                final Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                        Integer.class, existingEstudianteId, grupoId
                );
                if (count != null && count > 0) {
                    throw new ConflictException("El estudiante ya se encuentra matriculado en este grupo académico.");
                }
                final UUID estadoActivoId = jdbcTemplate.query(
                        "SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'",
                        rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
                );
                jdbcTemplate.update(
                        "INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado) VALUES (NEWID(), ?, ?, ?)",
                        existingEstudianteId, grupoId, estadoActivoId
                );

                // Asegurar sincronización en Keycloak para estudiante existente
                sincronizarCuentaKeycloakEstudiante(request);

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new RegistrarEstudianteResultadoDTO(true, "Estudiante matriculado exitosamente en el grupo."));
            }
        }

        final RegistrarEstudianteDTO dto = RegistrarEstudianteHttpMapper.toApplicationDTO(grupoId, request);
        final RegistrarEstudianteResultadoDTO resultado = registrarEstudianteInputPort != null
                ? registrarEstudianteInputPort.execute(dto)
                : new RegistrarEstudianteResultadoDTO(true, "Estudiante registrado exitosamente en el grupo.");

        // Crear/sincronizar cuenta en Keycloak tras registro exitoso del estudiante
        if (request.getNumeroIdentificacion() != null) {
            sincronizarCuentaKeycloakEstudiante(request);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    private void sincronizarCuentaKeycloakEstudiante(final RegistrarEstudianteRequest request) {
        if (identityProviderPort != null && request.getNumeroIdentificacion() != null) {
            try {
                final var cuentaDTO = new CrearCuentaIdentidadDTO(
                        String.valueOf(request.getNumeroIdentificacion()),
                        request.getCorreo(),
                        request.getPrimerNombre(),
                        request.getPrimerApellido(),
                        (request.getPassword() != null && !request.getPassword().isBlank()) ? request.getPassword() : "Test1234!",
                        "ESTUDIANTE"
                );
                final var cuentaCreada = identityProviderPort.crearCuenta(cuentaDTO);
                LOGGER.info("Cuenta de estudiante creada/sincronizada en Keycloak: username={}, idExterno={}",
                        cuentaDTO.username(), cuentaCreada.idExterno());
            } catch (IdentityProviderPort.IdentityProviderException ex) {
                LOGGER.error("No se pudo registrar/sincronizar la cuenta del estudiante en Keycloak. " +
                        "numeroIdentificacion={}, correo={}", request.getNumeroIdentificacion(), request.getCorreo(), ex);
            }
        }
    }

    @DeleteMapping("/{grupoId}/estudiantes/{estudianteId}")
    public ResponseEntity<ApiDataResponse<Boolean>> retirarEstudiante(
            @PathVariable final UUID grupoId,
            @PathVariable final UUID estudianteId
    ) {
        jdbcTemplate.update("DELETE FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?", estudianteId, grupoId);
        return ResponseEntity.ok(new ApiDataResponse<>(true, true));
    }
}
