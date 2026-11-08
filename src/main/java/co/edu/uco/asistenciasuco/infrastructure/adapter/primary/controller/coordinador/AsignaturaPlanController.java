package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.dto.PlanEstudioDTO;
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

    /** CULM-001 B2NM: malla de un plan (vinculos activos), paginada por usp_consultar_asignaturas_plan_paginado. PLA_002 (404) si el plan no existe o es ajeno. */
    @GetMapping({"/coordinador/planes-estudio/{planId}/asignaturas", "/decano/planes-estudio/{planId}/asignaturas"})
    public ResponseEntity<ApiPageResponse<AsignaturaPlanDTO>> malla(
            @PathVariable final UUID planId,
            @RequestParam(name = "semestre", required = false) final Integer semestre,
            @RequestParam(name = "caracter", required = false) final String caracter,
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano) {
        final var page = inputPort.consultarMalla(authenticatedUserResolver.requireAuthenticatedUserId(), planId, semestre, caracter, busqueda, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(page.elementos(), pagina, tamano, page.total()));
    }

    @GetMapping("/coordinador/asignaturas")
    public ResponseEntity<ApiPageResponse<AsignaturaPlanDTO>> asignaturasCoordinador(
            @RequestParam(name = "programaId", required = false) final UUID programaId,
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "estaActiva", required = false) final Boolean estaActiva,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano) {
        final var page = inputPort.consultarAsignaturasCoordinador(authenticatedUserResolver.requireAuthenticatedUserId(), programaId, busqueda, estaActiva, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(page.elementos(), pagina, tamano, page.total()));
    }

    @GetMapping("/coordinador/planes-estudio")
    public ResponseEntity<ApiPageResponse<PlanEstudioDTO>> planes(
            @RequestParam(name = "programaId", required = false) final UUID programaId,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano) {
        final var page = inputPort.consultarPlanes(authenticatedUserResolver.requireAuthenticatedUserId(), programaId, estaActivo, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(page.elementos(), pagina, tamano, page.total()));
    }

    /** CULM-001 D.9-a (F-DEC-003): detalle de un grupo de la facultad del decano. Inexistente o ajeno: 403 (SEC_002). */
    @GetMapping("/decano/grupos/{grupoId}")
    public ResponseEntity<ApiDataResponse<GrupoFacultadDTO>> grupoFacultad(@PathVariable final UUID grupoId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                inputPort.consultarGrupoFacultad(authenticatedUserResolver.requireAuthenticatedUserId(), grupoId)));
    }

    @GetMapping("/decano/grupos")
    public ResponseEntity<ApiPageResponse<GrupoFacultadDTO>> gruposFacultad(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico,
            @RequestParam(name = "programaId", required = false) final UUID programaId,
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano) {
        final var page = inputPort.consultarGruposFacultad(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico, programaId, busqueda, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(page.elementos(), pagina, tamano, page.total()));
    }
}
