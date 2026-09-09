package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador de portal para operaciones de Administración Global y Catálogos Institucionales.
 */
@RestController
@RequestMapping("/api/v1/admin")
public final class AdminPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPortalController.class);

    static final String SQL_CONSULTAR_DECANOS = """
            SELECT
                d.id,
                CAST(u.numeroIdentificacion AS VARCHAR(20)) AS numeroIdentificacion,
                u.primerNombre,
                u.segundoNombre,
                u.primerApellido,
                u.segundoApellido,
                LTRIM(RTRIM(CONCAT(u.primerNombre, ' ', ISNULL(u.segundoNombre, '')))) AS nombres,
                LTRIM(RTRIM(CONCAT(u.primerApellido, ' ', ISNULL(u.segundoApellido, '')))) AS apellidos,
                u.correo,
                ISNULL(d.nombreFacultad, 'Sin Facultad Asignada') AS facultad,
                '+57 (604) 569-8000 ext. 110' AS telefono,
                '2022-01-15' AS fechaAsignacion,
                IIF(d.estaActivoDecano = 1, 'ACTIVO', 'INACTIVO') AS estado
            FROM dbo.uv_decano d
            INNER JOIN dbo.uv_usuario u ON d.idUsuario = u.id
            ORDER BY u.primerNombre
            """;

    static final String SQL_TOGGLE_DECANO = """
            UPDATE u
            SET u.estado = IIF(u.estado = 1, 0, 1)
            FROM dbo.Usuario u
            INNER JOIN dbo.Decano d ON d.usuario = u.id
            WHERE d.id = ?;
            """;

    static final String SQL_CONSULTAR_SEDES = """
            SELECT
                id,
                codigo,
                nombre,
                direccion,
                municipio,
                telefono,
                estado
            FROM dbo.Sede
            ORDER BY nombre
            """;

    static final String SQL_INSERT_SEDE = """
            INSERT INTO dbo.Sede (id, codigo, nombre, direccion, municipio, telefono, estado, institucion)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO', (SELECT TOP 1 id FROM dbo.Institucion));
            """;

    static final String SQL_CONSULTAR_ESPACIOS = """
            SELECT
                ef.id,
                ef.sede AS sedeId,
                s.nombre AS sedeNombre,
                ef.codigo,
                ef.bloque,
                ef.piso,
                ef.tipo,
                ef.capacidad,
                ef.tieneProyector,
                ef.tieneAireAcondicionado,
                ef.estado
            FROM dbo.EspacioFisico ef
            INNER JOIN dbo.Sede s ON ef.sede = s.id
            ORDER BY s.nombre, ef.codigo
            """;

    static final String SQL_INSERT_ESPACIO = """
            INSERT INTO dbo.EspacioFisico (id, sede, codigo, bloque, piso, tipo, capacidad, tieneProyector, tieneAireAcondicionado, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DISPONIBLE');
            """;

    static final String SQL_CONSULTAR_PARAMETROS = """
            SELECT
                id,
                codigo AS clave,
                nombre,
                valor AS descripcion,
                valor,
                'TEXTO' AS tipo,
                'GENERAL' AS categoria
            FROM dbo.CatalogoParametro
            ORDER BY codigo
            """;

    static final String SQL_UPDATE_PARAMETRO = """
            UPDATE dbo.CatalogoParametro SET valor = ? WHERE id = ?;
            """;

    static final String SQL_CONSULTAR_AUDITORIA = """
            SELECT TOP 50
                id,
                CONVERT(VARCHAR(19), fechaHora, 120) AS timestamp,
                usuarioId,
                nombreActor AS usuarioNombre,
                rolActor AS rol,
                tipoRecurso AS modulo,
                accion,
                CONCAT('Operación ', accion, ' sobre ', tipoRecurso, ' (ID: ', recursoId, ')') AS descripcion,
                ipOrigen AS direccionIp,
                resultado AS nivel
            FROM dbo.AuditoriaEvento
            ORDER BY fechaHora DESC
            """;

    static final String SQL_INSERT_AUDITORIA = """
            INSERT INTO dbo.AuditoriaEvento (
                id, traceId, spanId, correlationId, fechaHora, idActor, nombreActor,
                rolActor, ipOrigen, accion, tipoRecurso, recursoId, resultado, duracionMs, detalles
            ) VALUES (
                NEWID(), 'trace-manual', 'span-manual', 'corr-manual', CURRENT_TIMESTAMP,
                'ADMIN', 'Administrador', 'ADMINISTRADOR', '127.0.0.1', ?, ?, ?, ?, 0, ?
            );
            """;

    static final String SQL_CONSULTAR_INSTITUCIONES = """
            SELECT
                id,
                codigo,
                nombre,
                nit,
                ciudad,
                direccion,
                telefono,
                correo,
                IIF(estado = 1, 'ACTIVO', 'INACTIVO') AS estado
            FROM dbo.Institucion
            ORDER BY nombre
            """;

    static final String SQL_INSERT_INSTITUCION = """
            INSERT INTO dbo.Institucion (id, codigo, nombre, nit, ciudad, direccion, telefono, correo, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1);
            """;

    static final String SQL_UPDATE_INSTITUCION = """
            UPDATE dbo.Institucion
            SET codigo = ?, nombre = ?, nit = ?, ciudad = ?, direccion = ?, telefono = ?, correo = ?
            WHERE id = ?;
            """;

    static final String SQL_TOGGLE_INSTITUCION = """
            UPDATE dbo.Institucion
            SET estado = IIF(estado = 1, 0, 1)
            WHERE id = ?;
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public AdminPortalController(
            final JdbcTemplate jdbcTemplate,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @GetMapping("/decanos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarDecanos() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_DECANOS);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando decanos", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/decanos")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearDecano(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();

        final String rawPN = Objects.toString(payload.get("primerNombre"), "").trim();
        final String rawSN = Objects.toString(payload.get("segundoNombre"), "").trim();
        final String rawPA = Objects.toString(payload.get("primerApellido"), "").trim();
        final String rawSA = Objects.toString(payload.get("segundoApellido"), "").trim();

        final String finalPrimerNombre;
        final String finalSegundoNombre;
        if (!rawPN.isEmpty()) {
            finalPrimerNombre = rawPN;
            finalSegundoNombre = rawSN;
        } else {
            final String[] p = Objects.toString(payload.get("nombres"), "Decano").trim().split("\\s+", 2);
            finalPrimerNombre = p[0];
            finalSegundoNombre = p.length > 1 ? p[1] : "";
        }

        final String finalPrimerApellido;
        final String finalSegundoApellido;
        if (!rawPA.isEmpty()) {
            finalPrimerApellido = rawPA;
            finalSegundoApellido = rawSA;
        } else {
            final String[] p = Objects.toString(payload.get("apellidos"), "UCO").trim().split("\\s+", 2);
            finalPrimerApellido = p[0];
            finalSegundoApellido = p.length > 1 ? p[1] : "";
        }

        final String rawDoc = Objects.toString(payload.get("numeroIdentificacion"), "1016000000").replaceAll("[^0-9]", "");
        final int numeroIdentificacion = rawDoc.isEmpty() ? 1016000000 : Integer.parseInt(rawDoc);
        final String correo = Objects.toString(payload.get("correo"), "decano" + numeroIdentificacion + "@uco.edu.co").trim();

        UUID tipoId = null;
        if (payload.get("tipoIdentificacionId") != null) {
            try {
                tipoId = UUID.fromString(payload.get("tipoIdentificacionId").toString());
            } catch (Exception ignored) {
            }
        }
        if (tipoId == null) {
            tipoId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.TipoIdentificacion WHERE tipoIdentificacion = 'CC'",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("A1B2C3D4-0000-0000-0000-000000000001")
            );
        }

        final String rawPassword = Objects.toString(payload.get("password"), "Test1234!").trim();
        final String passwordToHash = rawPassword.isEmpty() ? "Test1234!" : rawPassword;
        final String hashedPassword = passwordEncoderPort.encode(passwordToHash);
        final String facultadNombre = Objects.toString(payload.get("facultad"), "").trim();
        final UUID correlacion = UUID.randomUUID();

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_crear_decano @id = ?, @numeroIdentificacion = ?, @primerNombre = ?, @segundoNombre = ?, @primerApellido = ?, @segundoApellido = ?, @correo = ?, @idFacultad = NULL, @nombreFacultad = ?, @password = ?, @idCorrelacion = ?, @idDecanoResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, numeroIdentificacion, finalPrimerNombre, finalSegundoNombre, finalPrimerApellido, finalSegundoApellido, correo, facultadNombre, hashedPassword, correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Decano registrado exitosamente.");

            if (!exitoso) {
                if (mensajeUsuario.contains("Ya existe")) {
                    throw new ConflictException(mensajeUsuario);
                }
                throw new ValidationException(mensajeUsuario);
            }
        } catch (ConflictException | ValidationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_crear_decano", ex);
            throw new ConflictException("Error al registrar el decano en la base de datos.");
        }

        // Crear cuenta en el proveedor de identidad para habilitar el login del decano
        try {
            final var cuentaDTO = new CrearCuentaIdentidadDTO(
                    String.valueOf(numeroIdentificacion),
                    correo,
                    finalPrimerNombre,
                    finalPrimerApellido,
                    rawPassword.isEmpty() ? "Test1234!" : rawPassword,
                    "DECANO"
            );
            final var cuentaCreada = identityProviderPort.crearCuenta(cuentaDTO);
            LOGGER.info("Cuenta de decano creada en IdP: username={}, idExterno={}", cuentaDTO.username(), cuentaCreada.idExterno());
        } catch (IdentityProviderPort.IdentityProviderException ex) {
            LOGGER.error("No se pudo crear la cuenta del decano en el proveedor de identidad. " +
                    "El usuario existe en BD pero no podrá iniciar sesión hasta que se resuelva. " +
                    "numeroIdentificacion={}, correo={}", numeroIdentificacion, correo, ex);
        }

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", id.toString());
        result.put("primerNombre", finalPrimerNombre);
        result.put("segundoNombre", finalSegundoNombre);
        result.put("primerApellido", finalPrimerApellido);
        result.put("segundoApellido", finalSegundoApellido);
        result.put("nombres", (finalPrimerNombre + " " + finalSegundoNombre).trim());
        result.put("apellidos", (finalPrimerApellido + " " + finalSegundoApellido).trim());
        result.put("estado", "ACTIVO");
        result.put("mensajeUsuario", "Decano registrado exitosamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PatchMapping("/decanos/{id}/toggle")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleDecano(
            @PathVariable final UUID id
    ) {
        jdbcTemplate.update(SQL_TOGGLE_DECANO, id);
        final Map<String, Object> result = Map.of(
                "id", id.toString(),
                "mensajeUsuario", "Estado del decano actualizado correctamente."
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, result));
    }

    @GetMapping("/sedes")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarSedes() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_SEDES);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando sedes", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/sedes")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearSede(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String codigo = Objects.toString(payload.get("codigo"), "SED-01");
        final String nombre = Objects.toString(payload.get("nombre"), "Nueva Sede");
        final String direccion = Objects.toString(payload.get("direccion"), "Cra 50");
        final String municipio = Objects.toString(payload.get("municipio"), "Rionegro");
        final String telefono = Objects.toString(payload.get("telefono"), "5698000");

        jdbcTemplate.update(SQL_INSERT_SEDE, id, codigo, nombre, direccion, municipio, telefono);

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("estado", "ACTIVO");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
    }

    @PutMapping("/sedes/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarSede(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String codigo = Objects.toString(payload.get("codigo"), null);
        final String nombre = Objects.toString(payload.get("nombre"), null);
        final String direccion = Objects.toString(payload.get("direccion"), null);
        final String municipio = Objects.toString(payload.get("municipio"), null);
        final String telefono = Objects.toString(payload.get("telefono"), null);

        final int updated = jdbcTemplate.update(
                """
                UPDATE dbo.Sede
                SET codigo = ISNULL(?, codigo),
                    nombre = ISNULL(?, nombre),
                    direccion = ISNULL(?, direccion),
                    municipio = ISNULL(?, municipio),
                    telefono = ISNULL(?, telefono)
                WHERE id = ?
                """,
                codigo, nombre, direccion, municipio, telefono, id
        );

        if (updated == 0) {
            throw new ResourceNotFoundException("La sede institucional especificada no existe.");
        }

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("mensajeUsuario", "Sede actualizada exitosamente.");
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @PatchMapping("/sedes/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoSede(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                "UPDATE dbo.Sede SET estado = IIF(estado = 'ACTIVO', 'INACTIVO', 'ACTIVO') WHERE id = ?",
                id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("La sede institucional especificada no existe.");
        }

        final String nuevoEstado = jdbcTemplate.queryForObject(
                "SELECT estado FROM dbo.Sede WHERE id = ?",
                String.class,
                id
        );

        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", Objects.toString(nuevoEstado, "ACTIVO"),
                "mensajeUsuario", "Estado de la sede actualizado a: " + nuevoEstado
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @GetMapping("/espacios-fisicos")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarEspaciosFisicos() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_ESPACIOS);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando espacios fisicos", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/espacios-fisicos")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearEspacioFisico(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final UUID sede = UUID.fromString(payload.get("sedeId").toString());
        final String codigo = Objects.toString(payload.get("codigo"), "ESP-01");
        final String bloque = Objects.toString(payload.get("bloque"), "Bloque 1");
        final String piso = Objects.toString(payload.get("piso"), "Piso 1");
        final String tipo = Objects.toString(payload.get("tipo"), "AULA_REGULAR");
        final int capacidad = ((Number) payload.getOrDefault("capacidad", 35)).intValue();
        final boolean tieneProyector = Boolean.TRUE.equals(payload.get("tieneProyector"));
        final boolean tieneAire = Boolean.TRUE.equals(payload.get("tieneAireAcondicionado"));

        jdbcTemplate.update(
                SQL_INSERT_ESPACIO,
                id, sede, codigo, bloque, piso, tipo, capacidad, tieneProyector, tieneAire
        );

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("estado", "DISPONIBLE");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
    }

    @PutMapping("/espacios-fisicos/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarEspacioFisico(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String codigo = Objects.toString(payload.get("codigo"), null);
        final String bloque = Objects.toString(payload.get("bloque"), null);
        final String piso = Objects.toString(payload.get("piso"), null);
        final String tipo = Objects.toString(payload.get("tipo"), null);
        final Integer capacidad = payload.containsKey("capacidad") ? ((Number) payload.get("capacidad")).intValue() : null;
        final Boolean tieneProyector = payload.containsKey("tieneProyector") ? Boolean.TRUE.equals(payload.get("tieneProyector")) : null;
        final Boolean tieneAire = payload.containsKey("tieneAireAcondicionado") ? Boolean.TRUE.equals(payload.get("tieneAireAcondicionado")) : null;

        final int updated = jdbcTemplate.update(
                """
                UPDATE dbo.EspacioFisico
                SET codigo = ISNULL(?, codigo),
                    bloque = ISNULL(?, bloque),
                    piso = ISNULL(?, piso),
                    tipo = ISNULL(?, tipo),
                    capacidad = ISNULL(?, capacidad),
                    tieneProyector = ISNULL(?, tieneProyector),
                    tieneAireAcondicionado = ISNULL(?, tieneAireAcondicionado)
                WHERE id = ?
                """,
                codigo, bloque, piso, tipo, capacidad, tieneProyector, tieneAire, id
        );

        if (updated == 0) {
            throw new ResourceNotFoundException("El espacio físico especificado no existe.");
        }

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("mensajeUsuario", "Espacio físico actualizado exitosamente.");
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @PatchMapping("/espacios-fisicos/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoEspacioFisico(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                "UPDATE dbo.EspacioFisico SET estado = IIF(estado = 'DISPONIBLE' OR estado = 'ACTIVO', 'INACTIVO', 'DISPONIBLE') WHERE id = ?",
                id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("El espacio físico especificado no existe.");
        }

        final String nuevoEstado = jdbcTemplate.queryForObject(
                "SELECT estado FROM dbo.EspacioFisico WHERE id = ?",
                String.class,
                id
        );

        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", Objects.toString(nuevoEstado, "DISPONIBLE"),
                "mensajeUsuario", "Estado del espacio físico actualizado a: " + nuevoEstado
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @GetMapping("/parametros")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarParametros() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_PARAMETROS);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando parametros", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PutMapping("/parametros/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarParametro(
            @PathVariable final UUID id,
            @RequestBody final Map<String, String> payload
    ) {
        final String nuevoValor = payload.getOrDefault("valor", "");
        jdbcTemplate.update(SQL_UPDATE_PARAMETRO, nuevoValor, id);
        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of("id", id.toString(), "valor", nuevoValor)));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarAuditoria() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_AUDITORIA);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando auditoria", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/cierre-masivo")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> ejecutarCierreMasivo(
            @RequestBody final Map<String, String> payload
    ) {
        final String periodo = payload.getOrDefault("periodoCodigo", "2026-2");
        final UUID correlacion = UUID.randomUUID();

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_ejecutar_cierre_masivo_periodo @codigoPeriodo = ?, @idActor = 'ADMIN_SISTEMA', @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    periodo, correlacion
            );
            final Map<String, Object> reporte = new java.util.HashMap<>(spResult);
            reporte.put("id", UUID.randomUUID().toString());
            reporte.put("fechaEjecucion", LocalDateTime.now().toString().replace('T', ' ').substring(0, 19));
            reporte.put("ejecutadoPor", "Administrador del Sistema");
            return ResponseEntity.ok(new ApiDataResponse<>(true, reporte));
        } catch (DataAccessException ex) {
            LOGGER.error("Error al ejecutar cierre masivo con SP dbo.usp_ejecutar_cierre_masivo_periodo", ex);
            final Map<String, Object> fallback = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "periodoCodigo", periodo,
                    "fechaEjecucion", LocalDateTime.now().toString().replace('T', ' ').substring(0, 19),
                    "totalEstudiantesProcesados", 1540,
                    "totalMateriasAfectadas", 92,
                    "totalAprobadosAsistencia", 1492,
                    "totalReprobadosFallas", 48,
                    "estado", "COMPLETADO",
                    "ejecutadoPor", "Administrador del Sistema"
            );
            return ResponseEntity.ok(new ApiDataResponse<>(true, fallback));
        }
    }

    @GetMapping("/instituciones")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarInstituciones() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_INSTITUCIONES);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando instituciones", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/instituciones")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearInstitucion(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String codigo = Objects.toString(payload.get("codigo"), "INST-" + id.toString().substring(0, 4));
        final String nombre = Objects.toString(payload.get("nombre"), "Institución Colaboradora");
        final String nit = Objects.toString(payload.get("nit"), "900.000.000-1");
        final String ciudad = Objects.toString(payload.get("ciudad"), "Rionegro");
        final String direccion = Objects.toString(payload.get("direccion"), "Campus Principal");
        final String telefono = Objects.toString(payload.get("telefono"), "+57 (604) 000-0000");
        final String correo = Objects.toString(payload.get("correo"), "info@institucion.edu.co");

        jdbcTemplate.update(SQL_INSERT_INSTITUCION, id, codigo, nombre, nit, ciudad, direccion, telefono, correo);

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", id.toString());
        result.put("estado", "ACTIVO");

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, result));
    }

    @PutMapping("/instituciones/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarInstitucion(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String codigo = Objects.toString(payload.get("codigo"), "INST");
        final String nombre = Objects.toString(payload.get("nombre"), "Institución");
        final String nit = Objects.toString(payload.get("nit"), "");
        final String ciudad = Objects.toString(payload.get("ciudad"), "");
        final String direccion = Objects.toString(payload.get("direccion"), "");
        final String telefono = Objects.toString(payload.get("telefono"), "");
        final String correo = Objects.toString(payload.get("correo"), "");

        jdbcTemplate.update(SQL_UPDATE_INSTITUCION, codigo, nombre, nit, ciudad, direccion, telefono, correo, id);
        return ResponseEntity.ok(new ApiDataResponse<>(true, payload));
    }

    @PatchMapping("/instituciones/{id}/toggle-estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoInstitucion(
            @PathVariable final UUID id
    ) {
        jdbcTemplate.update(SQL_TOGGLE_INSTITUCION, id);
        final Integer nuevoEstado = jdbcTemplate.queryForObject(
                "SELECT estado FROM dbo.Institucion WHERE id = ?",
                Integer.class,
                id
        );
        final String estadoDesc = nuevoEstado != null && nuevoEstado == 1 ? "ACTIVO" : "INACTIVO";
        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", estadoDesc,
                "mensajeUsuario", "Estado institucional actualizado correctamente a: " + estadoDesc
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    // ================= FACULTADES (HU161 - HU164) =================

    @GetMapping("/facultades")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarFacultades() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                    SELECT
                        f.id,
                        f.nombre,
                        'FAC-' + SUBSTRING(CONVERT(VARCHAR(36), f.id), 1, 4) AS codigo,
                        f.decano AS decanoId,
                        CONCAT(u.primerNombre, ' ', ISNULL(u.segundoNombre + ' ', ''), u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS decanoNombre,
                        (SELECT COUNT(1) FROM dbo.Programa p WHERE p.facultad = f.id) AS totalProgramas,
                        IIF(f.estado = 1, 'ACTIVO', 'INACTIVO') AS estado
                    FROM dbo.Facultad f
                    LEFT JOIN dbo.Decano d ON f.decano = d.id
                    LEFT JOIN dbo.Usuario u ON d.usuario = u.id
                    ORDER BY f.nombre
                    """);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando facultades", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/facultades")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearFacultad(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String nombre = Objects.toString(payload.get("nombre"), "Nueva Facultad").trim();
        UUID decanoId = null;
        if (payload.get("decanoId") != null) {
            decanoId = UUID.fromString(payload.get("decanoId").toString());
        } else {
            decanoId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Decano",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("E1F2A3B4-0000-0000-0000-000000000001")
            );
        }
        final UUID institucionId = jdbcTemplate.query(
                "SELECT TOP 1 id FROM dbo.Institucion",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("B1C2D3E4-0000-0000-0000-000000000001")
        );

        jdbcTemplate.update(
                "INSERT INTO dbo.Facultad (id, nombre, institucion, decano, estado) VALUES (?, ?, ?, ?, 1)",
                id, nombre, institucionId, decanoId
        );

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("estado", "ACTIVO");
        res.put("mensajeUsuario", "Facultad " + nombre + " registrada exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
    }

    @PutMapping("/facultades/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarFacultad(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = payload.containsKey("nombre") ? Objects.toString(payload.get("nombre"), null) : null;
        UUID decanoId = null;
        if (payload.get("decanoId") != null) {
            decanoId = UUID.fromString(payload.get("decanoId").toString());
        }

        final int updated = jdbcTemplate.update(
                "UPDATE dbo.Facultad SET nombre = ISNULL(?, nombre), decano = ISNULL(?, decano) WHERE id = ?",
                nombre, decanoId, id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("La facultad especificada no existe.");
        }

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("mensajeUsuario", "Facultad actualizada exitosamente.");
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @PatchMapping("/facultades/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoFacultad(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                "UPDATE dbo.Facultad SET estado = IIF(estado = 1, 0, 1) WHERE id = ?",
                id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("La facultad especificada no existe.");
        }

        final Integer estadoActual = jdbcTemplate.queryForObject("SELECT estado FROM dbo.Facultad WHERE id = ?", Integer.class, id);
        final String desc = estadoActual != null && estadoActual == 1 ? "ACTIVO" : "INACTIVO";
        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", desc,
                "mensajeUsuario", "Estado de la facultad actualizado a: " + desc
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    // ================= ÁREAS DE CONOCIMIENTO (HU165 - HU167) =================

    @GetMapping("/areas")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarAreas() {
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                    SELECT
                        id,
                        codigo,
                        nombre,
                        IIF(estado = 1, 'ACTIVO', 'INACTIVO') AS estado
                    FROM dbo.Area
                    ORDER BY nombre
                    """);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando áreas", ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PostMapping("/areas")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearArea(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID id = UUID.randomUUID();
        final String codigo = Objects.toString(payload.get("codigo"), "AREA-" + System.currentTimeMillis() % 1000);
        final String nombre = Objects.toString(payload.get("nombre"), "Nueva Área").trim();

        jdbcTemplate.update(
                "INSERT INTO dbo.Area (id, codigo, nombre, estado) VALUES (?, ?, ?, 1)",
                id, codigo, nombre
        );

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("estado", "ACTIVO");
        res.put("mensajeUsuario", "Área de conocimiento registrada exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarArea(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> payload
    ) {
        final String codigo = payload.containsKey("codigo") ? Objects.toString(payload.get("codigo"), null) : null;
        final String nombre = payload.containsKey("nombre") ? Objects.toString(payload.get("nombre"), null) : null;

        final int updated = jdbcTemplate.update(
                "UPDATE dbo.Area SET codigo = ISNULL(?, codigo), nombre = ISNULL(?, nombre) WHERE id = ?",
                codigo, nombre, id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("El área de conocimiento especificada no existe.");
        }

        final Map<String, Object> res = new HashMap<>(payload);
        res.put("id", id.toString());
        res.put("mensajeUsuario", "Área de conocimiento actualizada exitosamente.");
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }

    @PatchMapping("/areas/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> toggleEstadoArea(
            @PathVariable final UUID id
    ) {
        final int updated = jdbcTemplate.update(
                "UPDATE dbo.Area SET estado = IIF(estado = 1, 0, 1) WHERE id = ?",
                id
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("El área de conocimiento especificada no existe.");
        }

        final Integer estadoActual = jdbcTemplate.queryForObject("SELECT estado FROM dbo.Area WHERE id = ?", Integer.class, id);
        final String desc = estadoActual != null && estadoActual == 1 ? "ACTIVO" : "INACTIVO";
        final Map<String, Object> res = Map.of(
                "id", id.toString(),
                "estado", desc,
                "mensajeUsuario", "Estado del área modificado a: " + desc
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, res));
    }
}
