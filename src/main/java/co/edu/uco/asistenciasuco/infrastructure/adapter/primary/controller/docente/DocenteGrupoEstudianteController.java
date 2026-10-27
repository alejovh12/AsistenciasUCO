package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto.RetirarEstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.RetirarEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * HU051: gestion de los estudiantes de un grupo por su docente titular. La titularidad la revalida la DB; un grupo
 * ajeno o un estudiante sin matricula activa responden 404 sin distinguir ambos casos.
 */
@RestController
@RequestMapping("/api/v1/docente/grupos")
public final class DocenteGrupoEstudianteController {

    private final RetirarEstudianteGrupoInputPort retirarEstudianteGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteGrupoEstudianteController(
            final RetirarEstudianteGrupoInputPort retirarEstudianteGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.retirarEstudianteGrupoInputPort = Objects.requireNonNull(retirarEstudianteGrupoInputPort,
                "RetirarEstudianteGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio.");
    }

    /** Retira al estudiante (matricula a "inactivo", libera el cupo) y avisa a estudiante, docente y coordinador. */
    @PostMapping("/{grupoId}/estudiantes/{estudianteId}/retiro")
    public ResponseEntity<ApiDataResponse<Void>> retirarEstudiante(
            @PathVariable final UUID grupoId,
            @PathVariable final UUID estudianteId,
            @RequestBody(required = false) final RetirarEstudianteRequest request
    ) {
        retirarEstudianteGrupoInputPort.execute(new RetirarEstudianteGrupoDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                grupoId,
                estudianteId,
                request == null ? null : request.getMotivo()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
