package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C, DEC-020/031): bandeja y resolucion del coordinador. Solo ve a los estudiantes de sus programas (la DB fuerza el
 * ambito) y solo resuelve si es el resolutor competente (no el proponente). Una revision ajena es 404 indistinguible.
 */
@RestController
@RequestMapping("/api/v1/coordinador/revisiones-cancelacion")
public final class CoordinadorRevisionCancelacionController {

    private static final DecisionRevisionCancelacionRequestValidator VALIDATOR = new DecisionRevisionCancelacionRequestValidator();

    private final ConsultarRevisionesCoordinadorInputPort consultar;
    private final ResolverRevisionCancelacionInputPort resolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorRevisionCancelacionController(
            final ConsultarRevisionesCoordinadorInputPort consultar,
            final ResolverRevisionCancelacionInputPort resolver,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultar = consultar;
        this.resolver = resolver;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<RevisionCancelacionResponse>> consultar(
            @RequestParam(name = "soloPendientes", defaultValue = "false") final boolean soloPendientes,
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "programaId", required = false) final UUID programaId,
            @RequestParam(name = "grupoId", required = false) final UUID grupoId,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final var resultado = consultar.execute(new ConsultarRevisionesCoordinadorInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), pagina, tamano, soloPendientes, estado, programaId, grupoId));
        return ResponseEntity.ok(ApiPageResponse.of(resultado.filas().stream().map(RevisionCancelacionHttpMapper::toResponse).toList(),
                pagina, tamano, resultado.total()));
    }

    @PatchMapping("/{revisionId}")
    @AuditableOperation(action = "RESOLVER_REVISION_CANCELACION", resourceType = "REVISION_CANCELACION", resourceIdPathVariable = "revisionId")
    public ResponseEntity<ApiDataResponse<DecisionRevisionCancelacionResponse>> resolver(
            @PathVariable final UUID revisionId,
            @RequestBody(required = false) final DecisionRevisionCancelacionRequest request
    ) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        final var resultado = resolver.execute(new ResolverRevisionCancelacionInputPort.Comando(
                revisionId, request.getDecision(), request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, RevisionCancelacionHttpMapper.toResponse(resultado)));
    }
}
