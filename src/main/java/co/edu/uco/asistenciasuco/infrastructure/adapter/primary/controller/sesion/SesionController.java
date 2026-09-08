package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper.SesionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CerrarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.ConsultarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CrearSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.UserScopeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con sesiones.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
public final class SesionController {

    private static final CrearSesionRequestValidator CREATE_SESSION_VALIDATOR = new CrearSesionRequestValidator();
    private static final ConsultarSesionRequestValidator CONSULT_SESSION_VALIDATOR = new ConsultarSesionRequestValidator();
    private static final CerrarSesionRequestValidator CLOSE_SESSION_VALIDATOR = new CerrarSesionRequestValidator();

    private final CrearSesionInputPort crearSesionInputPort;
    private final ConsultarSesionInputPort consultarSesionInputPort;
    private final CerrarSesionInputPort cerrarSesionInputPort;
    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;

    public SesionController(
            final CrearSesionInputPort crearSesionInputPort,
            final ConsultarSesionInputPort consultarSesionInputPort,
            final CerrarSesionInputPort cerrarSesionInputPort,
            final JdbcTemplate jdbcTemplate,
            final UserScopeService userScopeService
    ) {
        this.crearSesionInputPort = Objects.requireNonNull(crearSesionInputPort, "El puerto de entrada CrearSesionInputPort es obligatorio.");
        this.consultarSesionInputPort = Objects.requireNonNull(consultarSesionInputPort, "El puerto de entrada ConsultarSesionInputPort es obligatorio.");
        this.cerrarSesionInputPort = Objects.requireNonNull(cerrarSesionInputPort, "El puerto de entrada CerrarSesionInputPort es obligatorio.");
        this.jdbcTemplate = jdbcTemplate;
        this.userScopeService = userScopeService;
    }


    @PostMapping
    @AuditableOperation(action = "CREAR_SESION", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<ApiMessageResponse> crearSesion(@RequestBody final CrearSesionRequest request) {
        RequestValidationGuard.validate(CREATE_SESSION_VALIDATOR.validate(request));
        final CrearSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        crearSesionInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesion creada correctamente."));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<ApiListResponse<Map<String, Object>>> consultarSesionesPorGrupo(
            @PathVariable final UUID grupoId
    ) {
        final List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(
                """
                SELECT id, numero, nombre, descripcion, cerrada,
                       fechaHoraInicio, fechaHoraFin, aula, tipo, grupo
                FROM dbo.Sesion
                WHERE grupo = ?
                ORDER BY numero ASC, fechaHoraInicio ASC
                """,
                grupoId
        );
        return ResponseEntity.ok(new ApiListResponse<>(true, sesiones, sesiones.size()));
    }

    @GetMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesion(
            @PathVariable final UUID sesionId
    ) {
        final ConsultarSesionRequest request = new ConsultarSesionRequest();
        request.setSesion(sesionId);
        return executeConsultarSesion(request);
    }

    /**
     * @deprecated Compatibilidad temporal.
     * Consumir GET /api/v1/sesiones/{sesionId} o GET /api/v1/sesiones/grupo/{grupoId}.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/consultas")
    public ResponseEntity<?> consultarSesionLegacy(
            @RequestBody final ConsultarSesionRequest request
    ) {
        if (request != null && request.getSesion() != null) {
            final List<Map<String, Object>> sesionesPorGrupo = jdbcTemplate.queryForList(
                    """
                    SELECT id, numero, nombre, descripcion, cerrada,
                           fechaHoraInicio, fechaHoraFin, aula, tipo, grupo
                    FROM dbo.Sesion
                    WHERE grupo = ?
                    ORDER BY numero ASC, fechaHoraInicio ASC
                    """,
                    request.getSesion()
            );
            if (!sesionesPorGrupo.isEmpty()) {
                return ResponseEntity.ok(new ApiListResponse<>(true, sesionesPorGrupo, sesionesPorGrupo.size()));
            }
        }
        return executeConsultarSesion(request);
    }

    private ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> executeConsultarSesion(
            final ConsultarSesionRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_SESSION_VALIDATOR.validate(request));
        final ConsultarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        final SesionConsultadaDTO sesion = consultarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiDataResponse<>(true, sesion));
    }

    @PostMapping("/cierres")
    @AuditableOperation(action = "CERRAR_SESION", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> cerrarSesion(@RequestBody final CerrarSesionRequest request) {
        RequestValidationGuard.validate(CLOSE_SESSION_VALIDATOR.validate(request));
        final CerrarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        cerrarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiMessageResponse(true, "Sesion cerrada correctamente."));
    }

    @PutMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> actualizarSesion(
            @PathVariable final UUID sesionId,
            @RequestBody final Map<String, Object> payload
    ) {
        final String nombre = payload.containsKey("nombre") ? Objects.toString(payload.get("nombre"), null)
                : (payload.containsKey("tema") ? Objects.toString(payload.get("tema"), null) : null);
        final String fechaHoraInicio = payload.get("fechaHoraInicio") != null ? payload.get("fechaHoraInicio").toString() : null;
        final String fechaHoraFin = payload.get("fechaHoraFin") != null ? payload.get("fechaHoraFin").toString() : null;
        final String aula = payload.get("aula") != null ? payload.get("aula").toString() : null;
        final String descripcion = payload.get("descripcion") != null ? payload.get("descripcion").toString() : null;

        UUID docenteId = null;
        if (userScopeService != null) {
            docenteId = userScopeService.getAuthenticatedUserId()
                    .flatMap(userScopeService::findDocenteIdByUsuario)
                    .orElse(null);
        }

        final UUID correlacion = UUID.randomUUID();
        final Map<String, Object> spResult = jdbcTemplate.queryForMap(
                "EXEC dbo.usp_actualizar_sesion @id = ?, @nombre = ?, @fechaHoraInicio = ?, @fechaHoraFin = ?, @aula = ?, @descripcion = ?, @idDocente = ?, @idCorrelacion = ?, @mensajeUsuarioResultado = NULL, @mensajeTecnicoResultado = NULL, @estadoResultado = NULL",
                sesionId, nombre, fechaHoraInicio, fechaHoraFin, aula, descripcion, docenteId, correlacion
        );

        final boolean exitoso = spResult.get("estadoResultado") instanceof Boolean b ? b
                : (spResult.get("exitoso") instanceof Boolean b2 ? b2
                : ((Number) spResult.getOrDefault("estadoResultado", spResult.getOrDefault("exitoso", 1))).intValue() == 1);
        final String mensajeUsuario = Objects.toString(
                spResult.getOrDefault("mensajeUsuarioResultado", spResult.get("mensajeUsuario")),
                "Sesión actualizada."
        );
        if (!exitoso) {
            if (mensajeUsuario.contains("no existe")) throw new ResourceNotFoundException(mensajeUsuario);
            if (mensajeUsuario.contains("denegado")) throw new ForbiddenException(mensajeUsuario);
            throw new ValidationException(mensajeUsuario);
        }

        final Map<String, Object> result = new HashMap<>(payload);
        result.put("id", sesionId.toString());
        result.put("mensajeUsuario", mensajeUsuario);
        return ResponseEntity.ok(new ApiDataResponse<>(true, result));
    }
}

