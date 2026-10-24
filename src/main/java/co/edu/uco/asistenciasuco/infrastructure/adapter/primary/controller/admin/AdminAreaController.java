package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.ActualizarAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.CambiarEstadoAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.CrearAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto.CrearAreaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** HU151/HU152: escritura del catalogo de areas de conocimiento (lectura en AdminPortalController) (rol ADMINISTRADOR por SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/areas")
public final class AdminAreaController {

    private static final String NO_EXISTE = "El area de conocimiento especificada no existe en el sistema.";

    private final ConsultarAreasInputPort consultarInputPort;
    private final CrearAreaInputPort crearInputPort;
    private final ActualizarAreaInputPort actualizarInputPort;
    private final CambiarEstadoAreaInputPort cambiarEstadoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminAreaController(
            final ConsultarAreasInputPort consultarInputPort,
            final CrearAreaInputPort crearInputPort,
            final ActualizarAreaInputPort actualizarInputPort,
            final CambiarEstadoAreaInputPort cambiarEstadoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort);
        this.crearInputPort = Objects.requireNonNull(crearInputPort);
        this.actualizarInputPort = Objects.requireNonNull(actualizarInputPort);
        this.cambiarEstadoInputPort = Objects.requireNonNull(cambiarEstadoInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<AreaDTO>> crear(@RequestBody final CrearAreaRequest request) {
        final String codigo = request != null ? request.codigo() : null;
        crearInputPort.execute(new CrearAreaDTO(
                codigo,
                request != null ? request.nombre() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        final String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        final AreaDTO creado = consultarInputPort.execute().stream()
                .filter(t -> codigoNormalizado.equals(t.codigo()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(NO_EXISTE));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<AreaDTO>> actualizar(
            @PathVariable final UUID id,
            @RequestBody final ActualizarAreaRequest request
    ) {
        actualizarInputPort.execute(new ActualizarAreaDTO(
                id,
                request != null ? request.nombre() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releer(id)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiDataResponse<AreaDTO>> cambiarEstado(
            @PathVariable final UUID id,
            @RequestBody final CambiarEstadoAreaRequest request
    ) {
        cambiarEstadoInputPort.execute(new CambiarEstadoAreaDTO(
                id,
                request != null ? request.activa() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releer(id)));
    }

    private AreaDTO releer(final UUID id) {
        return consultarInputPort.execute().stream()
                .filter(t -> id.equals(t.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(NO_EXISTE));
    }

    public record CrearAreaRequest(String codigo, String nombre) {
    }

    public record ActualizarAreaRequest(String nombre) {
    }

    public record CambiarEstadoAreaRequest(Boolean activa) {
    }
}
