package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.ConsultarCatalogosSistemaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.CategoriaJustificacionCatalogoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.MotivoCancelacionCatalogoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Catalogos definidos por el sistema, de solo lectura para el ADMINISTRADOR ({@code /api/v1/admin/**}): categorias de justificacion y
 * motivos de cancelacion de sesion. Nadie los administra desde la plataforma.
 */
@RestController
@RequestMapping("/api/v1/admin")
public final class AdminCatalogosSistemaController {

    private final ConsultarCatalogosSistemaInputPort inputPort;

    public AdminCatalogosSistemaController(final ConsultarCatalogosSistemaInputPort inputPort) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultarCatalogosSistemaInputPort es obligatorio.");
    }

    @GetMapping("/categorias-justificacion")
    public ResponseEntity<ApiListResponse<CategoriaJustificacionCatalogoDTO>> categoriasJustificacion() {
        final List<CategoriaJustificacionCatalogoDTO> datos = inputPort.consultarCategoriasJustificacion();
        return ResponseEntity.ok(new ApiListResponse<>(true, datos, datos.size()));
    }

    @GetMapping("/motivos-cancelacion-sesion")
    public ResponseEntity<ApiListResponse<MotivoCancelacionCatalogoDTO>> motivosCancelacion() {
        final List<MotivoCancelacionCatalogoDTO> datos = inputPort.consultarMotivosCancelacion();
        return ResponseEntity.ok(new ApiListResponse<>(true, datos, datos.size()));
    }
}
