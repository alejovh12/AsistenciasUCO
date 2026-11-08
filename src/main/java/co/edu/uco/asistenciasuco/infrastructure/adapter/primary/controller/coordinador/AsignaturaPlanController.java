package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Asignaturas por plan (CULM-001 B2NM, DEC-019): asociar, desvincular, asociables, caracteres y programas de un grupo. */
@RestController
@RequestMapping("/api/v1")
public final class AsignaturaPlanController {

    private final AsignaturaPlanInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AsignaturaPlanController(final AsignaturaPlanInputPort inputPort, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = inputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/coordinador/planes-estudio/{planId}/asignaturas-asociadas")
    public ResponseEntity<ApiDataResponse<Void>> asociar(@PathVariable final UUID planId, @RequestBody final AsociarAsignaturaPlanDTO request) {
        inputPort.asociar(authenticatedUserResolver.requireAuthenticatedUserId(), planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, null));
    }

    @DeleteMapping("/coordinador/planes-estudio/{planId}/asignaturas/{asigId}")
    public ResponseEntity<ApiDataResponse<Void>> desvincular(@PathVariable final UUID planId, @PathVariable final UUID asigId) {
        inputPort.desvincular(authenticatedUserResolver.requireAuthenticatedUserId(), planId, asigId);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @GetMapping("/coordinador/planes-estudio/{planId}/asignaturas-asociables")
    public ResponseEntity<ApiPageResponse<AsignaturaAsociableDTO>> asociables(
            @PathVariable final UUID planId,
            @RequestParam(required = false) final String busqueda,
            @RequestParam(defaultValue = "1") final int pagina,
            @RequestParam(defaultValue = "20") final int tamano) {
        final var page = inputPort.consultarAsociables(authenticatedUserResolver.requireAuthenticatedUserId(), planId, busqueda, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(page.elementos(), pagina, tamano, page.total()));
    }

    @GetMapping("/coordinador/caracteres-asignatura")
    public ResponseEntity<ApiListResponse<CaracterAsignaturaDTO>> caracteres() {
        final var data = inputPort.consultarCaracteres();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/grupos/{grupoId}/programas")
    public ResponseEntity<ApiListResponse<ProgramaResumenDTO>> programasGrupo(@PathVariable final UUID grupoId) {
        final var data = inputPort.consultarProgramasGrupo(authenticatedUserResolver.requireAuthenticatedUserId(), grupoId);
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
}
