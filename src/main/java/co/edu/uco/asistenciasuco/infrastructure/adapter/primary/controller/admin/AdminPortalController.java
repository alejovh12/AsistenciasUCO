package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.dto.CambiarEstadoDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.CambiarEstadoDecanoInputPort;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.ActualizarFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto.ActualizarFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.CambiarEstadoFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto.FacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto.InstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.FacultadNuevaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.CrearDecanoResponse;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.ConsultarHistorialCierresMasivosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.dto.EjecutarCierreMasivoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.CierreMasivoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.CrearDecanoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.FacultadRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationCodes;
import jakarta.validation.constraints.Positive;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

@RestController
@RequestMapping("/api/v1/admin")
public final class AdminPortalController {

    private static final CrearDecanoRequestValidator CREAR_DECANO_VALIDATOR = new CrearDecanoRequestValidator();
    private static final CierreMasivoRequestValidator CIERRE_MASIVO_VALIDATOR = new CierreMasivoRequestValidator();
    private static final FacultadRequestValidator FACULTAD_VALIDATOR = new FacultadRequestValidator();

    private final ConsultarDecanosInputPort consultarDecanosInputPort;
    private final CrearDecanoInputPort crearDecanoInputPort;
    private final EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort;
    private final ConsultarInstitucionesInputPort consultarInstitucionesInputPort;
    private final ConsultarFacultadesInputPort consultarFacultadesInputPort;
    private final CrearFacultadInputPort crearFacultadInputPort;
    private final ActualizarFacultadInputPort actualizarFacultadInputPort;
    private final CambiarEstadoFacultadInputPort cambiarEstadoFacultadInputPort;
    private final ConsultarAreasInputPort consultarAreasInputPort;
    private final CambiarEstadoDecanoInputPort cambiarEstadoDecanoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ConsultarHistorialCierresMasivosInputPort consultarHistorialCierresMasivosInputPort;

    public AdminPortalController(
            final ConsultarDecanosInputPort consultarDecanosInputPort,
            final CrearDecanoInputPort crearDecanoInputPort,
            final EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort,
            final ConsultarInstitucionesInputPort consultarInstitucionesInputPort,
            final ConsultarFacultadesInputPort consultarFacultadesInputPort,
            final CrearFacultadInputPort crearFacultadInputPort,
            final ActualizarFacultadInputPort actualizarFacultadInputPort,
            final CambiarEstadoFacultadInputPort cambiarEstadoFacultadInputPort,
            final ConsultarAreasInputPort consultarAreasInputPort,
            final CambiarEstadoDecanoInputPort cambiarEstadoDecanoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this(consultarDecanosInputPort, crearDecanoInputPort, ejecutarCierreMasivoInputPort,
                consultarInstitucionesInputPort, consultarFacultadesInputPort, crearFacultadInputPort,
                actualizarFacultadInputPort, cambiarEstadoFacultadInputPort, consultarAreasInputPort,
                cambiarEstadoDecanoInputPort, authenticatedUserResolver, null);
    }

    @Autowired // dos constructores publicos (el corto lo usan las pruebas): Spring debe usar el completo
    public AdminPortalController(
            final ConsultarDecanosInputPort consultarDecanosInputPort,
            final CrearDecanoInputPort crearDecanoInputPort,
            final EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort,
            final ConsultarInstitucionesInputPort consultarInstitucionesInputPort,
            final ConsultarFacultadesInputPort consultarFacultadesInputPort,
            final CrearFacultadInputPort crearFacultadInputPort,
            final ActualizarFacultadInputPort actualizarFacultadInputPort,
            final CambiarEstadoFacultadInputPort cambiarEstadoFacultadInputPort,
            final ConsultarAreasInputPort consultarAreasInputPort,
            final CambiarEstadoDecanoInputPort cambiarEstadoDecanoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver,
            final ConsultarHistorialCierresMasivosInputPort consultarHistorialCierresMasivosInputPort
    ) {
        this.consultarDecanosInputPort = consultarDecanosInputPort;
        this.crearDecanoInputPort = crearDecanoInputPort;
        this.ejecutarCierreMasivoInputPort = ejecutarCierreMasivoInputPort;
        this.consultarInstitucionesInputPort = consultarInstitucionesInputPort;
        this.consultarFacultadesInputPort = consultarFacultadesInputPort;
        this.crearFacultadInputPort = crearFacultadInputPort;
        this.actualizarFacultadInputPort = actualizarFacultadInputPort;
        this.cambiarEstadoFacultadInputPort = cambiarEstadoFacultadInputPort;
        this.consultarAreasInputPort = consultarAreasInputPort;
        this.cambiarEstadoDecanoInputPort = cambiarEstadoDecanoInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.consultarHistorialCierresMasivosInputPort = consultarHistorialCierresMasivosInputPort;
    }

    @GetMapping("/decanos")
    public ResponseEntity<ApiListResponse<DecanoDTO>> consultarDecanos() {
        final var data = consultarDecanosInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @AuditableOperation(action = "CREAR_DECANO", resourceType = "DECANO", metadataRequestFields = {"idFacultad"})
    @PostMapping("/decanos")
    public ResponseEntity<ApiDataResponse<CrearDecanoResponse>> crearDecano(@Valid @RequestBody final CrearDecanoRequest request) {
        RequestValidationGuard.validate(CREAR_DECANO_VALIDATOR.validate(request));
        final var facultadNueva = request.facultadNueva();
        final var resultado = crearDecanoInputPort.execute(new CrearDecanoDTO(
                request.tipoIdentificacionId(), request.numeroIdentificacion(), request.primerNombre(),
                request.segundoNombre(), request.primerApellido(), request.segundoApellido(), request.correo(),
                request.password(), request.idFacultad(), authenticatedUserResolver.requireAuthenticatedUserId(),
                facultadNueva == null ? null : new FacultadNuevaDTO(
                        facultadNueva.nombre(), facultadNueva.codigo(), facultadNueva.idInstitucion())
        ));
        // DEC-065 (5): 201 con el decano y la facultad (existente vinculada o recien creada) en una sola respuesta.
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true,
                new CrearDecanoResponse(resultado.idDecano(), resultado.idFacultad(), resultado.nombreFacultad())));
    }

    /** CULM-001 A.2-8 (F-ADM-002): 200 con la fila del decano y su estado nuevo; 409 DEC_002; 404 DEC_001. */
    @PatchMapping("/decanos/{id}/toggle")
    @AuditableOperation(action = "CAMBIAR_ESTADO_DECANO", resourceType = "DECANO", resourceIdPathVariable = "id")
    public ResponseEntity<ApiDataResponse<DecanoDTO>> toggleDecano(@PathVariable final UUID id) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, cambiarEstadoDecanoInputPort.execute(
                new CambiarEstadoDecanoDTO(id, authenticatedUserResolver.requireAuthenticatedUserId()))));
    }

    // HU020/HU140 descartadas (decision de John 2026-11-06): sedes y espacios fisicos no se piden; se retiran sus stubs 501.

    // HU168 (CULM-001 D.3): los parametros viven en AdminParametroController (GET paginado, PUT y cambios).

    @AuditableOperation(action = "CIERRE_MASIVO_PERIODO", resourceType = "PERIODO_ACADEMICO", resourceIdRequestField = "idPeriodoAcademico")
    @PostMapping("/cierre-masivo")
    public ResponseEntity<ApiMessageResponse> ejecutarCierreMasivo(@Valid @RequestBody final CierreMasivoRequest request) {
        RequestValidationGuard.validate(CIERRE_MASIVO_VALIDATOR.validate(request));
        ejecutarCierreMasivoInputPort.execute(new EjecutarCierreMasivoDTO(
                request.idPeriodoAcademico(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiMessageResponse(true, "Cierre masivo solicitado correctamente."));
    }

    /** F-ADM-006: historial de reportes de cierre masivo de periodo academico. */
    @GetMapping("/cierre-masivo/historial")
    public ResponseEntity<ApiListResponse<CierrePeriodoReporteDTO>> consultarHistorialCierre() {
        final var reportes = consultarHistorialCierresMasivosInputPort != null
                ? consultarHistorialCierresMasivosInputPort.execute()
                : java.util.List.<CierrePeriodoReporteDTO>of();
        return ResponseEntity.ok(new ApiListResponse<>(true, reportes, reportes.size()));
    }

    @GetMapping("/instituciones")
    public ResponseEntity<ApiListResponse<InstitucionDTO>> consultarInstituciones() {
        final var data = consultarInstitucionesInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    // HU161/HU162/HU164 descartadas (decision de John 2026-11-05): Institucion es un catalogo de solo consulta.

    @GetMapping("/facultades")
    public ResponseEntity<ApiListResponse<FacultadDTO>> consultarFacultades() {
        final var data = consultarFacultadesInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @AuditableOperation(action = "CREAR_FACULTAD", resourceType = "FACULTAD", resourceIdResponseField = "id")
    @PostMapping("/facultades")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> crearFacultad(@Valid @RequestBody final CrearFacultadRequest request) {
        RequestValidationGuard.validate(FACULTAD_VALIDATOR.validarCreacion(request));
        final UUID actorId = authenticatedUserResolver.requireAuthenticatedUserId();
        final UUID facultadId = UUID.randomUUID();
        final String nombre = request != null ? request.nombre() : null;
        final UUID idInstitucion = request != null ? request.idInstitucion() : null;
        final UUID idDecano = request != null ? request.idDecano() : null;

        crearFacultadInputPort.execute(new CrearFacultadDTO(
                facultadId,
                nombre,
                idInstitucion,
                idDecano,
                actorId
        ));

        final FacultadDTO dto = consultarFacultadesInputPort.execute().stream()
                .filter(f -> f.id().equals(facultadId))
                .findFirst()
                .orElseGet(() -> new FacultadDTO(
                        facultadId,
                        nombre,
                        idInstitucion,
                        null,
                        idDecano,
                        null,
                        true,
                        "SI"
                ));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, dto));
    }
    @AuditableOperation(action = "ACTUALIZAR_FACULTAD", resourceType = "FACULTAD", resourceIdPathVariable = "id", metadataRequestFields = {"nombre", "idDecano"})
    @PutMapping("/facultades/{id}")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> actualizarFacultad(
            @PathVariable final UUID id,
            @Valid @RequestBody final ActualizarFacultadRequest request
    ) {
        RequestValidationGuard.validate(FACULTAD_VALIDATOR.validarActualizacion(request));
        actualizarFacultadInputPort.execute(new ActualizarFacultadDTO(
                id,
                request != null ? request.nombre() : null,
                request != null ? request.idDecano() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releerFacultad(id)));
    }
    @AuditableOperation(action = "CAMBIAR_ESTADO_FACULTAD", resourceType = "FACULTAD", resourceIdPathVariable = "id", metadataRequestFields = {"activa"})
    @PatchMapping("/facultades/{id}/estado")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> cambiarEstadoFacultad(
            @PathVariable final UUID id,
            @Valid @RequestBody final CambiarEstadoFacultadRequest request
    ) {
        cambiarEstadoFacultadInputPort.execute(new CambiarEstadoFacultadDTO(
                id,
                request != null ? request.activa() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releerFacultad(id)));
    }

    @GetMapping("/areas")
    public ResponseEntity<ApiListResponse<AreaDTO>> consultarAreas() {
        final var data = consultarAreasInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    private FacultadDTO releerFacultad(final UUID id) {
        return consultarFacultadesInputPort.execute().stream()
                .filter(f -> id.equals(f.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(MensajeUsuarioErrorCode.NOF_APP_013));
    }

    public record CrearDecanoRequest(
            @NotNull(message = "El tipo de identificacion es obligatorio.") @NonEmptyUuid(message = "El tipo de identificacion es obligatorio.", payload = BeanValidationCodes.Required.class) UUID tipoIdentificacionId,
            @NotNull(message = "El numero de identificacion es obligatorio y debe ser positivo.") @Positive(message = "El numero de identificacion es obligatorio y debe ser positivo.", payload = BeanValidationCodes.InvalidFormat.class) Integer numeroIdentificacion,
            @NotBlank(message = "El campo primerNombre es obligatorio.") @Size(max = 50, message = "El campo primerNombre no puede superar 50 caracteres.") String primerNombre,
            @Size(max = 50, message = "El campo segundoNombre no puede superar 50 caracteres.") String segundoNombre,
            @NotBlank(message = "El campo primerApellido es obligatorio.") @Size(max = 50, message = "El campo primerApellido no puede superar 50 caracteres.") String primerApellido,
            @Size(max = 50, message = "El campo segundoApellido no puede superar 50 caracteres.") String segundoApellido,
            @NotBlank(message = "El correo es obligatorio.") @Size(max = 255, message = "El correo no puede superar 255 caracteres.") @Pattern(regexp = BeanValidationPatterns.EMAIL_OR_BLANK, message = "El correo no tiene un formato valido.") String correo,
            String password,
            UUID idFacultad,
            @Valid FacultadNuevaRequest facultadNueva
    ) {
    }

    /** Facultad que se crea junto con el decano (DEC-065 (5)); excluyente con {@code idFacultad}. */
    public record FacultadNuevaRequest(
            @NotBlank(message = "El nombre de la facultad debe tener entre 3 y 50 caracteres.", payload = BeanValidationCodes.InvalidLength.class)
            @Size(min = 3, max = 50, message = "El nombre de la facultad debe tener entre 3 y 50 caracteres.") String nombre,
            @Size(max = 20, message = "El codigo de la facultad no puede superar 20 caracteres.") String codigo,
            @NotNull(message = "La institucion de la facultad es obligatoria.") @NonEmptyUuid(message = "La institucion de la facultad es obligatoria.", payload = BeanValidationCodes.Required.class) UUID idInstitucion
    ) {
    }

    public record CrearFacultadRequest(
            @NotBlank(message = "El nombre de la facultad es obligatorio.") @Size(min = 3, max = 50, message = "El nombre de la facultad debe tener entre 3 y 50 caracteres.") String nombre,
            String codigo,
            UUID idInstitucion,
            @NotNull(message = "El decano titular de la facultad es obligatorio.") @NonEmptyUuid(message = "El decano titular de la facultad es obligatorio.", payload = BeanValidationCodes.Required.class) UUID idDecano
    ) {
    }

    public record ActualizarFacultadRequest(
            @Size(max = 50, message = "El nombre de la facultad debe tener entre 3 y 50 caracteres.") String nombre,
            UUID idDecano
    ) {
    }

    public record CambiarEstadoFacultadRequest(Boolean activa) {
    }

    public record CierreMasivoRequest(
            @NotNull(message = "El periodo academico es obligatorio.") @NonEmptyUuid(message = "El periodo academico es obligatorio.", payload = BeanValidationCodes.Required.class) UUID idPeriodoAcademico
    ) {
    }
}
