package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.ActualizarProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.CambiarEstadoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.mapper.ProgramaAcademicoHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CambiarEstadoProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.ProgramaAcademicoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response.ProgramaAcademicoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation.ProgramaAcademicoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Programas academicos de la facultad del decano autenticado (HU076, HU077, HU078, HU108). */
@RestController
@RequestMapping("/api/v1/decano/programas")
public final class DecanoProgramaController {

    private final CrearProgramaInputPort crearPrograma;
    private final ActualizarProgramaInputPort actualizarPrograma;
    private final CambiarEstadoProgramaInputPort cambiarEstadoPrograma;
    private final ConsultarProgramasInputPort consultarProgramas;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoProgramaController(
            final CrearProgramaInputPort crearPrograma,
            final ActualizarProgramaInputPort actualizarPrograma,
            final CambiarEstadoProgramaInputPort cambiarEstadoPrograma,
            final ConsultarProgramasInputPort consultarProgramas,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearPrograma = Objects.requireNonNull(crearPrograma, "CrearProgramaInputPort es obligatorio.");
        this.actualizarPrograma = Objects.requireNonNull(actualizarPrograma, "ActualizarProgramaInputPort es obligatorio.");
        this.cambiarEstadoPrograma = Objects.requireNonNull(cambiarEstadoPrograma, "CambiarEstadoProgramaInputPort es obligatorio.");
        this.consultarProgramas = Objects.requireNonNull(consultarProgramas, "ConsultarProgramasInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<ProgramaAcademicoResponse>> consultar(
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "nombre", required = false) final String nombre,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "idTipoPrograma", required = false) final UUID idTipoPrograma
    ) {
        final ProgramasPaginaDTO resultado = consultarProgramas.execute(new ConsultarProgramasDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), nombre, estaActivo, idTipoPrograma, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(
                true,
                resultado.programas().stream().map(ProgramaAcademicoHttpMapper::toResponse).toList(),
                resultado.totalRegistros()));
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<ProgramaAcademicoResponse>> crear(@RequestBody final ProgramaAcademicoRequest request) {
        ProgramaAcademicoRequestValidator.validar(request);
        final ProgramaAcademicoDTO creado = crearPrograma.execute(new CrearProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), request.getNombre(), request.getIdTipoPrograma(),
                request.getIdCoordinador(), request.getIdModalidad(), request.getIdJornada()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiDataResponse<>(true, ProgramaAcademicoHttpMapper.toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<ProgramaAcademicoResponse>> actualizar(
            @PathVariable final UUID id,
            @RequestBody final ProgramaAcademicoRequest request
    ) {
        ProgramaAcademicoRequestValidator.validar(request);
        final ProgramaAcademicoDTO actualizado = actualizarPrograma.execute(new ActualizarProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), id, request.getNombre(), request.getIdTipoPrograma(),
                request.getIdCoordinador(), request.getIdModalidad(), request.getIdJornada()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, ProgramaAcademicoHttpMapper.toResponse(actualizado)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiDataResponse<ProgramaAcademicoResponse>> cambiarEstado(
            @PathVariable final UUID id,
            @RequestBody final CambiarEstadoProgramaRequest request
    ) {
        ProgramaAcademicoRequestValidator.validar(request);
        final ProgramaAcademicoDTO resultado = cambiarEstadoPrograma.execute(new CambiarEstadoProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), id, request.getEstaActivo()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, ProgramaAcademicoHttpMapper.toResponse(resultado)));
    }
}
