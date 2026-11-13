package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.primaryports.ConsultarRevisionesDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.request.DecisionRevisionCancelacionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.DecisionRevisionCancelacionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.RevisionCancelacionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.support.RevisionCancelacionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.DecisionRevisionCancelacionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C): el docente titular mantiene o propone la cancelacion de un estudiante en perdida. DEC-065 (1): ademas consulta las
 * revisiones de sus grupos (pendientes de su decision o en seguimiento); el ambito lo fuerza el SP (grupo ajeno = SEC_002).
 */
@RestController
@RequestMapping("/api/v1/docente/revisiones-cancelacion")
public final class DocenteRevisionCancelacionController {

    private static final DecisionRevisionCancelacionRequestValidator VALIDATOR = new DecisionRevisionCancelacionRequestValidator();

    private final DecidirRevisionCancelacionInputPort decidir;
    private final ConsultarRevisionesDocenteInputPort consultar;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteRevisionCancelacionController(
            final DecidirRevisionCancelacionInputPort decidir,
            final ConsultarRevisionesDocenteInputPort consultar,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.decidir = decidir;
        this.consultar = consultar;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<RevisionCancelacionResponse>> consultar(
            @RequestParam(name = "soloPendientes", defaultValue = "true") final boolean soloPendientes,
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "grupoId", required = false) final UUID grupoId,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final var resultado = consultar.execute(new ConsultarRevisionesDocenteInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), pagina, tamano, soloPendientes, estado, grupoId));
        return ResponseEntity.ok(ApiPageResponse.of(resultado.filas().stream().map(RevisionCancelacionHttpMapper::toResponse).toList(),
                pagina, tamano, resultado.total()));
    }

    @PatchMapping("/{revisionId}")
    @AuditableOperation(action = "DECIDIR_REVISION_CANCELACION", resourceType = "REVISION_CANCELACION", resourceIdPathVariable = "revisionId")
    public ResponseEntity<ApiDataResponse<DecisionRevisionCancelacionResponse>> decidir(
            @PathVariable final UUID revisionId,
            @Valid @RequestBody(required = false) final DecisionRevisionCancelacionRequest request
    ) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        Objects.requireNonNull(request, "La decision es obligatoria.");
        final var resultado = decidir.execute(new DecidirRevisionCancelacionInputPort.Comando(
                revisionId, request.getDecision(), request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, RevisionCancelacionHttpMapper.toResponse(resultado)));
    }
}
