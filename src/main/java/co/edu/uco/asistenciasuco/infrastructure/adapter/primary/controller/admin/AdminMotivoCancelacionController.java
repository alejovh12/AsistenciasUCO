package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.GestionarMotivoCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.MotivoCancelacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.GuardarMotivoCancelacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.CrearMotivoCancelacionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.ActualizarMotivoCancelacionRequest;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataMensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.CambiarEstadoCatalogoRequest;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Administracion del catalogo de motivos de cancelacion de sesion (migracion 65 de la BD). Solo ADMINISTRADOR ({@code /api/v1/admin/**}); los SP
 * lo revalidan (SEC_001). Sin borrado: el estado se cambia con PATCH. La oferta a los otros roles no cambia.
 */
@RestController
@RequestMapping("/api/v1/admin/motivos-cancelacion-sesion")
public final class AdminMotivoCancelacionController {

    private final GestionarMotivoCancelacionInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminMotivoCancelacionController(final GestionarMotivoCancelacionInputPort inputPort, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort, "GestionarMotivoCancelacionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    /** Todos los registros, activos e inactivos. */
    @GetMapping
    public ResponseEntity<ApiListResponse<MotivoCancelacionAdminDTO>> listar() {
        final List<MotivoCancelacionAdminDTO> datos = inputPort.consultar();
        return ResponseEntity.ok(new ApiListResponse<>(true, datos, datos.size()));
    }

    @AuditableOperation(action = "CREAR_MOTIVO_CANCELACION_SESION", resourceType = "MOTIVO_CANCELACION_SESION", metadataRequestFields = {"codigo", "nombre", "orden"})
    @PostMapping
    public ResponseEntity<ApiDataMensajeResponse<IdResponse>> crear(@Valid @RequestBody final CrearMotivoCancelacionRequest request) {
        final ResultadoCatalogoAdminDTO r = inputPort.crear(new GuardarMotivoCancelacionDTO(null, request.codigo(), request.nombre(), request.orden(), usuario()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataMensajeResponse<>(true, new IdResponse(r.id()), r.mensajeUsuario()));
    }

    @AuditableOperation(action = "ACTUALIZAR_MOTIVO_CANCELACION_SESION", resourceType = "MOTIVO_CANCELACION_SESION", resourceIdPathVariable = "id", metadataRequestFields = {"nombre", "orden"})
    @PutMapping("/{id}")
    public ResponseEntity<ApiDataMensajeResponse<IdResponse>> actualizar(
            @PathVariable("id") final UUID id, @Valid @RequestBody final ActualizarMotivoCancelacionRequest request) {
        final ResultadoCatalogoAdminDTO r = inputPort.actualizar(new GuardarMotivoCancelacionDTO(id, null, request.nombre(), request.orden(), usuario()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, new IdResponse(r.id()), r.mensajeUsuario()));
    }

    @AuditableOperation(action = "CAMBIAR_ESTADO_MOTIVO_CANCELACION_SESION", resourceType = "MOTIVO_CANCELACION_SESION", resourceIdPathVariable = "id", metadataRequestFields = {"estaActivo"})
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiDataMensajeResponse<EstadoResponse>> cambiarEstado(
            @PathVariable("id") final UUID id, @Valid @RequestBody final CambiarEstadoCatalogoRequest request) {
        final ResultadoCatalogoAdminDTO r = inputPort.cambiarEstado(id, request.estaActivo(), usuario());
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, new EstadoResponse(r.id(), request.estaActivo()), r.mensajeUsuario()));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }

    /** Identificador del registro creado o actualizado. */
    public record IdResponse(UUID id) {
    }

    /** Registro con el estado en que quedo. */
    public record EstadoResponse(UUID id, boolean estaActivo) {
    }
}
