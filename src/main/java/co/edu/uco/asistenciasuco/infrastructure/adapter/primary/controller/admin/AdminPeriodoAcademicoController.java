package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.GestionarPeriodoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.GuardarPeriodoAcademicoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.PeriodoAdminDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.PaginacionRequestValidator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * HU096/HU097/HU098: alta, modificacion y eliminacion de periodos academicos. El periodo es unico por institucion:
 * solo lo gestiona el ADMINISTRADOR ({@code /api/v1/admin/**} en SecurityConfig); DECANO y COORDINADOR lo consultan por
 * {@code GET /api/v1/coordinador/periodos-academicos}.
 */
@RestController
@RequestMapping("/api/v1/admin/periodos-academicos")
public final class AdminPeriodoAcademicoController {

    private final GestionarPeriodoAcademicoInputPort gestionarPeriodoAcademicoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminPeriodoAcademicoController(
            final GestionarPeriodoAcademicoInputPort gestionarPeriodoAcademicoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.gestionarPeriodoAcademicoInputPort = Objects.requireNonNull(gestionarPeriodoAcademicoInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @AuditableOperation(action = "CREAR_PERIODO_ACADEMICO", resourceType = "PERIODO_ACADEMICO", metadataRequestFields = {"nombre"})
    @PostMapping
    public ResponseEntity<ApiDataResponse<PeriodoCreadoResponse>> crear(@Valid @RequestBody final GuardarPeriodoAcademicoRequest request) {
        final UUID id = gestionarPeriodoAcademicoInputPort.crear(toDTO(null, request));
        // CULM-001 D.9-a (F-ADM-030): 201 con el periodo creado (antes 200 con datos nulos).
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true,
                new PeriodoCreadoResponse(id, request.getNombre().trim(), request.getFechaInicio(), request.getFechaFin())));
    }

    /** CULM-001 D.9-a (F-ADM-030): periodos de la institucion del administrador, paginados, con esVigente. */
    @GetMapping
    public ResponseEntity<ApiPageResponse<PeriodoAdminDTO>> consultar(
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano);
        final var resultado = gestionarPeriodoAcademicoInputPort.consultar(
                authenticatedUserResolver.requireAuthenticatedUserId(), pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(resultado.elementos(), solicitada.numero(), solicitada.tamano(), resultado.total()));
    }

    /** Periodo creado: identificador generado por el backend y los datos recibidos. */
    public record PeriodoCreadoResponse(UUID id, String nombre, java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin) {
    }

    @AuditableOperation(action = "ACTUALIZAR_PERIODO_ACADEMICO", resourceType = "PERIODO_ACADEMICO", resourceIdPathVariable = "id", metadataRequestFields = {"nombre"})
    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizar(
            @PathVariable final UUID id,
            @Valid @RequestBody final GuardarPeriodoAcademicoRequest request
    ) {
        gestionarPeriodoAcademicoInputPort.actualizar(toDTO(id, request));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @AuditableOperation(action = "ELIMINAR_PERIODO_ACADEMICO", resourceType = "PERIODO_ACADEMICO", resourceIdPathVariable = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiDataResponse<Void>> eliminar(@PathVariable final UUID id) {
        gestionarPeriodoAcademicoInputPort.eliminar(id, authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    private GuardarPeriodoAcademicoDTO toDTO(final UUID id, final GuardarPeriodoAcademicoRequest request) {
        return new GuardarPeriodoAcademicoDTO(
                id,
                request == null ? null : request.getNombre(),
                request == null ? null : request.getFechaInicio(),
                request == null ? null : request.getFechaFin(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
    }
}
