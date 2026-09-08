package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador de portal para operaciones del Decano restringidas a su Facultad.
 */
@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoPortalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecanoPortalController.class);

    static final String SQL_CONSULTAR_COORDINADORES_FACULTAD = """
            SELECT
                c.id,
                CAST(u.numeroIdentificacion AS VARCHAR(20)) AS numeroIdentificacion,
                u.primerNombre AS nombres,
                CONCAT(u.primerApellido, ' ', ISNULL(u.segundoApellido, '')) AS apellidos,
                u.correo,
                c.nombreFacultad AS facultad,
                c.nombrePrograma AS programaAcademico,
                (SELECT COUNT(DISTINCT g.docente) FROM dbo.Grupo g 
                 INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
                 INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                 INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
                 WHERE pe.programa = c.idPrograma) AS totalDocentes,
                (SELECT COUNT(g.id) FROM dbo.Grupo g 
                 INNER JOIN dbo.Asignatura a ON g.asignatura = a.id
                 INNER JOIN dbo.SemestrePlanEstudio spe ON a.semestrePlanEstudio = spe.id
                 INNER JOIN dbo.PlanEstudio pe ON spe.planEstudio = pe.id
                 WHERE pe.programa = c.idPrograma) AS totalGrupos,
                IIF(c.estaActivoCoordinador = 1, 'ACTIVO', 'INACTIVO') AS estado
            FROM dbo.uv_coordinador c
            INNER JOIN dbo.uv_usuario u ON c.idUsuario = u.id
            WHERE c.idFacultad = ?
            ORDER BY u.primerNombre
            """;

    static final String SQL_CHECK_COORDINADOR_FACULTAD = """
            SELECT p.facultad
            FROM dbo.Coordinador c
            INNER JOIN dbo.Programa p ON p.coordinador = c.id
            WHERE c.id = ?
            """;

    static final String SQL_TOGGLE_COORDINADOR = """
            UPDATE u
            SET u.estado = IIF(u.estado = 1, 0, 1)
            FROM dbo.Usuario u
            INNER JOIN dbo.Coordinador c ON c.usuario = u.id
            WHERE c.id = ?;
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public DecanoPortalController(
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.userScopeService = Objects.requireNonNull(userScopeService, "UserScopeService es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @GetMapping("/coordinadores")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarCoordinadores() {
        final UUID facultadId = resolveCurrentFacultadId();
        try {
            final List<Map<String, Object>> list = jdbcTemplate.queryForList(SQL_CONSULTAR_COORDINADORES_FACULTAD, facultadId);
            return ResponseEntity.ok(new ApiListResponse<>(true, list, list.size()));
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando coordinadores para facultadId={}", facultadId, ex);
            return ResponseEntity.ok(new ApiListResponse<>(true, List.of(), 0));
        }
    }

    @PatchMapping("/coordinadores/{id}/estado")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> cambiarEstadoCoordinador(
            @PathVariable final UUID id
    ) {
        final UUID facultadId = resolveCurrentFacultadId();

        final UUID coordFacultad = jdbcTemplate.query(
                SQL_CHECK_COORDINADOR_FACULTAD,
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                id
        );

        if (coordFacultad == null) {
            throw new ResourceNotFoundException("Coordinador no encontrado.");
        }

        if (!coordFacultad.equals(facultadId)) {
            throw new ForbiddenException("El coordinador no pertenece a su facultad.");
        }

        jdbcTemplate.update(SQL_TOGGLE_COORDINADOR, id);

        final Map<String, Object> result = Map.of(
                "id", id.toString(),
                "mensajeUsuario", "Estado del coordinador actualizado exitosamente."
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, result));
    }

    @PostMapping("/coordinadores")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> crearCoordinador(
            @RequestBody final Map<String, Object> payload
    ) {
        final UUID facultadId = resolveCurrentFacultadId();
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
            final String nombres = Objects.toString(payload.get("nombres"), "Coordinador").trim();
            final String[] nPartes = nombres.split("\\s+", 2);
            primerNombre = nPartes[0];
            segundoNombre = nPartes.length > 1 ? nPartes[1] : "";
        }

        final String primerApellido;
        final String segundoApellido;
        if (!rawPA.isEmpty()) {
            primerApellido = rawPA;
            segundoApellido = rawSA;
        } else {
            final String apellidos = Objects.toString(payload.get("apellidos"), "UCO").trim();
            final String[] aPartes = apellidos.split("\\s+", 2);
            primerApellido = aPartes[0];
            segundoApellido = aPartes.length > 1 ? aPartes[1] : "";
        }

        final String rawDoc = Objects.toString(payload.get("numeroIdentificacion"), "1018000000").replaceAll("[^0-9]", "");
        final int numeroIdentificacion = rawDoc.isEmpty() ? 1018000000 : Integer.parseInt(rawDoc);
        final String correo = Objects.toString(payload.get("correo"), "coord" + numeroIdentificacion + "@uco.edu.co").trim();

        // Resolver programa de la facultad
        UUID programaId = null;
        if (payload.get("programaId") != null) {
            programaId = UUID.fromString(payload.get("programaId").toString());
        } else if (payload.get("programaAcademico") != null) {
            final String progNom = payload.get("programaAcademico").toString();
            programaId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Programa WHERE facultad = ? AND LOWER(nombre) LIKE LOWER(?)",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    facultadId, "%" + progNom + "%"
            );
        }
        if (programaId == null) {
            programaId = jdbcTemplate.query(
                    "SELECT TOP 1 id FROM dbo.Programa WHERE facultad = ?",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : UUID.fromString("B2C3D4E5-0000-0000-0000-000000000001"),
                    facultadId
            );
        }

        final UUID id = UUID.randomUUID();
        final UUID correlacion = UUID.randomUUID();

        final String rawPassword = Objects.toString(payload.get("password"), "Test1234!").trim();
        final String passwordToHash = rawPassword.isEmpty() ? "Test1234!" : rawPassword;
        final String hashedPassword = passwordEncoderPort.encode(passwordToHash);

        try {
            final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                    "EXEC dbo.usp_crear_coordinador @id = ?, @numeroIdentificacion = ?, @primerNombre = ?, @segundoNombre = ?, @primerApellido = ?, @segundoApellido = ?, @correo = ?, @idPrograma = ?, @idFacultad = ?, @password = ?, @idCorrelacion = ?, @idCoordinadorResultado = NULL, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                    id, numeroIdentificacion, primerNombre, segundoNombre, primerApellido, segundoApellido, correo, programaId, facultadId, hashedPassword, correlacion
            );

            final boolean exitoso = spResult.get("exitoso") instanceof Boolean b ? b
                    : ((Number) spResult.getOrDefault("exitoso", 1)).intValue() == 1;
            final String mensajeUsuario = Objects.toString(spResult.get("mensajeUsuario"), "Coordinador registrado exitosamente.");

            if (!exitoso) {
                if (mensajeUsuario.contains("denegado")) throw new ForbiddenException(mensajeUsuario);
                throw new ConflictException(mensajeUsuario);
            }

            final Map<String, Object> res = new HashMap<>(payload);
            res.put("id", id.toString());
            res.put("estado", "ACTIVO");
            res.put("mensajeUsuario", mensajeUsuario);

            // Crear cuenta en el proveedor de identidad para habilitar el login del coordinador
            try {
                final var cuentaDTO = new CrearCuentaIdentidadDTO(
                        String.valueOf(numeroIdentificacion),
                        correo,
                        primerNombre,
                        primerApellido,
                        rawPassword.isEmpty() ? "Test1234!" : rawPassword,
                        "coordinador"
                );
                final var cuentaCreada = identityProviderPort.crearCuenta(cuentaDTO);
                LOGGER.info("Cuenta de coordinador creada en IdP: username={}, idExterno={}", cuentaDTO.username(), cuentaCreada.idExterno());
            } catch (IdentityProviderPort.IdentityProviderException ex) {
                LOGGER.error("No se pudo crear la cuenta del coordinador en el proveedor de identidad. " +
                        "El usuario existe en BD pero no podrá iniciar sesión hasta que se resuelva. " +
                        "numeroIdentificacion={}, correo={}", numeroIdentificacion, correo, ex);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, res));
        } catch (ConflictException | ForbiddenException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOGGER.error("Error ejecutando usp_crear_coordinador", ex);
            throw new ConflictException("Error al registrar coordinador: " + ex.getMostSpecificCause().getMessage());
        }
    }

    private UUID resolveCurrentFacultadId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000001")); // Fallback decano de prueba

        return userScopeService.findFacultadIdByDecanoUsuario(usuarioId)
                .orElse(UUID.fromString("A2B3C4D5-0000-0000-0000-000000000001")); // Facultad de Ingeniería
    }
}
