package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response.CrearUsuarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper.CrearUsuarioHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation.CrearUsuarioRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.UserScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con usuarios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public final class UsuarioController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioController.class);
    private static final CrearUsuarioRequestValidator CREATE_USER_VALIDATOR = new CrearUsuarioRequestValidator();

    private final CrearUsuarioInputPort crearUsuarioInputPort;
    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;
    private final IdentityProviderPort identityProviderPort;

    @Autowired
    public UsuarioController(
            final CrearUsuarioInputPort crearUsuarioInputPort,
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService,
            final IdentityProviderPort identityProviderPort
    ) {
        this.crearUsuarioInputPort = Objects.requireNonNull(crearUsuarioInputPort, "El puerto de entrada CrearUsuarioInputPort es obligatorio.");
        this.jdbcTemplate = jdbcTemplate;
        this.userScopeService = userScopeService;
        this.identityProviderPort = identityProviderPort;
    }

    public UsuarioController(final CrearUsuarioInputPort crearUsuarioInputPort) {
        this(crearUsuarioInputPort, null, null, null);
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> consultarPerfilUsuarioAutenticado() {
        if (jdbcTemplate == null || userScopeService == null) {
            return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of()));
        }

        final Optional<UUID> authUserId = userScopeService.getAuthenticatedUserId();
        if (authUserId.isEmpty()) {
            throw new ResourceNotFoundException("No se encontró el usuario autenticado en la base de datos.");
        }

        final UUID usuarioId = authUserId.get();

        final String sql = """
                SELECT TOP 1
                    u.id,
                    u.numeroIdentificacion,
                    u.primerNombre,
                    u.segundoNombre,
                    u.primerApellido,
                    u.segundoApellido,
                    u.correo,
                    u.estado,
                    ti.tipoIdentificacion AS tipoDocumento
                FROM dbo.Usuario u
                LEFT JOIN dbo.TipoIdentificacion ti ON u.tipoIdIdentificacion = ti.id
                WHERE u.id = ?
                """;

        final Map<String, Object> usuario = jdbcTemplate.query(
                sql,
                rs -> {
                    if (rs.next()) {
                        final Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getString("id"));
                        map.put("numeroIdentificacion", rs.getInt("numeroIdentificacion"));
                        map.put("primerNombre", rs.getString("primerNombre"));
                        map.put("segundoNombre", rs.getString("segundoNombre"));
                        map.put("primerApellido", rs.getString("primerApellido"));
                        map.put("segundoApellido", rs.getString("segundoApellido"));
                        map.put("correo", rs.getString("correo"));
                        map.put("email", rs.getString("correo"));
                        map.put("estado", rs.getBoolean("estado") ? "active" : "inactive");
                        map.put("tipoDocumento", rs.getString("tipoDocumento"));

                        final String pNom = rs.getString("primerNombre") != null ? rs.getString("primerNombre") : "";
                        final String sNom = rs.getString("segundoNombre") != null ? rs.getString("segundoNombre") : "";
                        final String pApe = rs.getString("primerApellido") != null ? rs.getString("primerApellido") : "";
                        final String sApe = rs.getString("segundoApellido") != null ? rs.getString("segundoApellido") : "";

                        final String nombreCompleto = String.join(" ",
                                java.util.Arrays.asList(pNom, sNom, pApe, sApe))
                                .replaceAll("\\s+", " ").trim();

                        map.put("name", nombreCompleto);

                        return map;
                    }
                    return null;
                },
                usuarioId
        );

        if (usuario == null) {
            throw new ResourceNotFoundException("Registro de usuario no encontrado en la base de datos.");
        }

        return ResponseEntity.ok(new ApiDataResponse<>(true, usuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarPerfilUsuarioAutenticado(
            @RequestBody final Map<String, Object> payload
    ) {
        if (jdbcTemplate == null || userScopeService == null) {
            return ResponseEntity.ok(new ApiDataResponse<>(true, payload));
        }

        final Optional<UUID> authUserId = userScopeService.getAuthenticatedUserId();
        if (authUserId.isEmpty()) {
            throw new ResourceNotFoundException("No se encontró el usuario autenticado en la base de datos.");
        }

        final UUID usuarioId = authUserId.get();

        final String primerNombre = Objects.toString(payload.get("primerNombre"), "").trim();
        final String segundoNombre = Objects.toString(payload.get("segundoNombre"), "").trim();
        final String primerApellido = Objects.toString(payload.get("primerApellido"), "").trim();
        final String segundoApellido = Objects.toString(payload.get("segundoApellido"), "").trim();

        if (primerNombre.isEmpty() || primerApellido.isEmpty()) {
            throw new IllegalArgumentException("El primer nombre y el primer apellido son obligatorios.");
        }

        final String updateSql = """
                UPDATE dbo.Usuario
                SET primerNombre = ?,
                    segundoNombre = ?,
                    primerApellido = ?,
                    segundoApellido = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                updateSql,
                primerNombre,
                segundoNombre.isEmpty() ? null : segundoNombre,
                primerApellido,
                segundoApellido.isEmpty() ? null : segundoApellido,
                usuarioId
        );

        return consultarPerfilUsuarioAutenticado();
    }

    @PostMapping
    @AuditableOperation(
            action = "CREAR_USUARIO",
            resourceType = "USUARIO"
    )
    public ResponseEntity<CrearUsuarioResponse> crearUsuario(@RequestBody final CrearUsuarioRequest request) {
        RequestValidationGuard.validate(CREATE_USER_VALIDATOR.validate(request));
        final CrearUsuarioDTO dto = CrearUsuarioHttpMapper.toApplicationDTO(request);
        final CrearUsuarioResultadoDTO resultado = crearUsuarioInputPort.execute(dto);
        if (!ObjectHelper.isNull(resultado.getUsuarioId())) {
            AuditRequestAttributes.storeResourceId(resultado.getUsuarioId().toString());
        }

        // Asegurar que el usuario quede registrado en dbo.Estudiante
        if (jdbcTemplate != null && resultado.getUsuarioId() != null) {
            try {
                final Integer countEst = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM dbo.Estudiante WHERE usuario = ?",
                        Integer.class, resultado.getUsuarioId()
                );
                if (countEst == null || countEst == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO dbo.Estudiante (id, usuario) VALUES (NEWID(), ?)",
                            resultado.getUsuarioId()
                    );
                    LOGGER.info("Perfil de estudiante registrado en dbo.Estudiante para usuarioId={}", resultado.getUsuarioId());
                }
            } catch (Exception ex) {
                LOGGER.warn("No fue posible registrar perfil en dbo.Estudiante para usuarioId={}: {}",
                        resultado.getUsuarioId(), ex.getMessage());
            }
        }

        // Crear cuenta en el proveedor de identidad para habilitar el login del estudiante
        if (identityProviderPort != null) {
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
                LOGGER.info("Cuenta de estudiante creada en IdP: username={}, idExterno={}",
                        cuentaDTO.username(), cuentaCreada.idExterno());
            } catch (IdentityProviderPort.IdentityProviderException ex) {
                LOGGER.error("No se pudo crear la cuenta del estudiante en el proveedor de identidad. " +
                        "El usuario existe en BD pero no podrá iniciar sesión hasta que se resuelva. " +
                        "numeroIdentificacion={}", request.getNumeroIdentificacion(), ex);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(CrearUsuarioResponse.from(resultado));
    }
}
