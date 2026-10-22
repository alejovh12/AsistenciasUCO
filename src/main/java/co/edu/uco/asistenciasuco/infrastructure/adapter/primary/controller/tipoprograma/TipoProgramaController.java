package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoprograma;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** HU023: consumo transversal del catalogo de tipos de programa (solo activos, cualquier usuario autenticado). */
@RestController
@RequestMapping("/api/v1/tipos-programa")
public final class TipoProgramaController {

    private final ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort;

    public TipoProgramaController(final ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort) {
        this.consultarTiposProgramaInputPort = Objects.requireNonNull(consultarTiposProgramaInputPort,
                "El puerto de entrada ConsultarTiposProgramaInputPort es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<TipoProgramaDTO>> consultarTiposPrograma() {
        final var data = consultarTiposProgramaInputPort.consultarActivos();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
}
