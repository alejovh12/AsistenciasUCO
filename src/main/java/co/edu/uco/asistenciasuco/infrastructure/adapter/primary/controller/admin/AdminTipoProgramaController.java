package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.ActualizarTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.CambiarEstadoTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.CrearTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto.CrearTipoProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

/** HU154: administracion del catalogo de tipos de programa (rol ADMINISTRADOR por SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/tipos-programa")
public final class AdminTipoProgramaController {

    private static final String NO_EXISTE = "El tipo de programa especificado no existe en el sistema.";

    private final ConsultarTiposProgramaInputPort consultarInputPort;
    private final CrearTipoProgramaInputPort crearInputPort;
    private final ActualizarTipoProgramaInputPort actualizarInputPort;
    private final CambiarEstadoTipoProgramaInputPort cambiarEstadoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminTipoProgramaController(
            final ConsultarTiposProgramaInputPort consultarInputPort,
            final CrearTipoProgramaInputPort crearInputPort,
            final ActualizarTipoProgramaInputPort actualizarInputPort,
            final CambiarEstadoTipoProgramaInputPort cambiarEstadoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort);
        this.crearInputPort = Objects.requireNonNull(crearInputPort);
        this.actualizarInputPort = Objects.requireNonNull(actualizarInputPort);
        this.cambiarEstadoInputPort = Objects.requireNonNull(cambiarEstadoInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<TipoProgramaDTO>> listar() {
        final var data = consultarInputPort.consultarTodos();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<TipoProgramaDTO>> crear(@RequestBody final CrearTipoProgramaRequest request) {
        final String codigo = request != null ? request.codigo() : null;
        crearInputPort.execute(new CrearTipoProgramaDTO(
                codigo,
                request != null ? request.nombre() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        final String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        final TipoProgramaDTO creado = consultarInputPort.consultarTodos().stream()
                .filter(t -> codigoNormalizado.equals(t.codigo()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(NO_EXISTE));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<TipoProgramaDTO>> actualizar(
            @PathVariable final UUID id,
            @RequestBody final ActualizarTipoProgramaRequest request
    ) {
        actualizarInputPort.execute(new ActualizarTipoProgramaDTO(
                id,
                request != null ? request.nombre() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releer(id)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiDataResponse<TipoProgramaDTO>> cambiarEstado(
            @PathVariable final UUID id,
            @RequestBody final CambiarEstadoTipoProgramaRequest request
    ) {
        cambiarEstadoInputPort.execute(new CambiarEstadoTipoProgramaDTO(
                id,
                request != null ? request.activo() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releer(id)));
    }

    private TipoProgramaDTO releer(final UUID id) {
        return consultarInputPort.consultarTodos().stream()
                .filter(t -> id.equals(t.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(NO_EXISTE));
    }

    public record CrearTipoProgramaRequest(String codigo, String nombre) {
    }

    public record ActualizarTipoProgramaRequest(String nombre) {
    }

    public record CambiarEstadoTipoProgramaRequest(Boolean activo) {
    }
}
