package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.GestionarCategoriaJustificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.CategoriaJustificacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.GuardarCategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.CrearCategoriaJustificacionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.ActualizarCategoriaJustificacionRequest;
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
 * Administracion del catalogo de categorias de justificacion (migracion 65 de la BD). Solo ADMINISTRADOR ({@code /api/v1/admin/**}); los SP
 * lo revalidan (SEC_001). Sin borrado: el estado se cambia con PATCH. La oferta a los otros roles no cambia.
 */
@RestController
@RequestMapping("/api/v1/admin/categorias-justificacion")
public final class AdminCategoriaJustificacionController {

    private final GestionarCategoriaJustificacionInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminCategoriaJustificacionController(final GestionarCategoriaJustificacionInputPort inputPort, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort, "GestionarCategoriaJustificacionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    /** Todos los registros, activos e inactivos. */
    @GetMapping
    public ResponseEntity<ApiListResponse<CategoriaJustificacionAdminDTO>> listar() {
        final List<CategoriaJustificacionAdminDTO> datos = inputPort.consultar();
        return ResponseEntity.ok(new ApiListResponse<>(true, datos, datos.size()));
    }

    @AuditableOperation(action = "CREAR_CATEGORIA_JUSTIFICACION", resourceType = "CATEGORIA_JUSTIFICACION", metadataRequestFields = {"codigo", "nombre", "requiereSoporte"})
    @PostMapping
    public ResponseEntity<ApiDataMensajeResponse<IdResponse>> crear(@Valid @RequestBody final CrearCategoriaJustificacionRequest request) {
        final ResultadoCatalogoAdminDTO r = inputPort.crear(new GuardarCategoriaJustificacionDTO(null, request.codigo(), request.nombre(), request.descripcion(), request.requiereSoporte(), usuario()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataMensajeResponse<>(true, new IdResponse(r.id()), r.mensajeUsuario()));
    }

    @AuditableOperation(action = "ACTUALIZAR_CATEGORIA_JUSTIFICACION", resourceType = "CATEGORIA_JUSTIFICACION", resourceIdPathVariable = "id", metadataRequestFields = {"nombre", "requiereSoporte"})
    @PutMapping("/{id}")
    public ResponseEntity<ApiDataMensajeResponse<IdResponse>> actualizar(
            @PathVariable("id") final UUID id, @Valid @RequestBody final ActualizarCategoriaJustificacionRequest request) {
        final ResultadoCatalogoAdminDTO r = inputPort.actualizar(new GuardarCategoriaJustificacionDTO(id, null, request.nombre(), request.descripcion(), request.requiereSoporte(), usuario()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, new IdResponse(r.id()), r.mensajeUsuario()));
    }

    @AuditableOperation(action = "CAMBIAR_ESTADO_CATEGORIA_JUSTIFICACION", resourceType = "CATEGORIA_JUSTIFICACION", resourceIdPathVariable = "id", metadataRequestFields = {"estaActivo"})
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
