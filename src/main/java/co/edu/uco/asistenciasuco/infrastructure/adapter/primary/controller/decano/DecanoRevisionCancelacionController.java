package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
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

import java.util.Objects;
import java.util.UUID;

/**
 * DEC-065 (1): el decano solo consulta (todas las revisiones de su facultad). La resolucion es del coordinador: el PATCH se conserva en la
 * ruta, pero la DB responde RCN_016 y el backend lo expone como 403.
 */
@RestController
@RequestMapping("/api/v1/decano/revisiones-cancelacion")
public final class DecanoRevisionCancelacionController {

    private static final DecisionRevisionCancelacionRequestValidator VALIDATOR = new DecisionRevisionCancelacionRequestValidator();

    private final ConsultarRevisionesDecanoInputPort consultar;
    private final ResolverRevisionCancelacionInputPort resolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoRevisionCancelacionController(
            final ConsultarRevisionesDecanoInputPort consultar,
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
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final var resultado = consultar.execute(new ConsultarRevisionesDecanoInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), pagina, tamano, soloPendientes, estado, programaId));
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
        Objects.requireNonNull(request, "La decision es obligatoria.");
        final var resultado = resolver.execute(new ResolverRevisionCancelacionInputPort.Comando(
                revisionId, request.getDecision(), request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, RevisionCancelacionHttpMapper.toResponse(resultado)));
    }
}
