package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.request.DecisionRevisionCancelacionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.DecisionRevisionCancelacionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.support.RevisionCancelacionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.DecisionRevisionCancelacionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/** CULM-001 C.1 (HU026C): el docente titular mantiene o propone la cancelacion de un estudiante en perdida. La bandeja del docente es C.2. */
@RestController
@RequestMapping("/api/v1/docente/revisiones-cancelacion")
public final class DocenteRevisionCancelacionController {

    private static final DecisionRevisionCancelacionRequestValidator VALIDATOR = new DecisionRevisionCancelacionRequestValidator();

    private final DecidirRevisionCancelacionInputPort decidir;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteRevisionCancelacionController(final DecidirRevisionCancelacionInputPort decidir, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.decidir = decidir;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping("/{revisionId}")
    @AuditableOperation(action = "DECIDIR_REVISION_CANCELACION", resourceType = "REVISION_CANCELACION", resourceIdPathVariable = "revisionId")
    public ResponseEntity<ApiDataResponse<DecisionRevisionCancelacionResponse>> decidir(
            @PathVariable final UUID revisionId,
            @RequestBody(required = false) final DecisionRevisionCancelacionRequest request
    ) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        Objects.requireNonNull(request, "La decision es obligatoria.");
        final var resultado = decidir.execute(new DecidirRevisionCancelacionInputPort.Comando(
                revisionId, request.getDecision(), request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, RevisionCancelacionHttpMapper.toResponse(resultado)));
    }
}
