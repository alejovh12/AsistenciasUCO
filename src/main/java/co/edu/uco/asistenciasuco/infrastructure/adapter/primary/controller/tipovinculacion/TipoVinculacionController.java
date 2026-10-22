package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipovinculacion;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.ConsultarTiposVinculacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.dto.TipoVinculacionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** Catalogo de tipos de vinculacion activos (HU090). */
@RestController
@RequestMapping("/api/v1/tipos-vinculacion")
public final class TipoVinculacionController {

    private final ConsultarTiposVinculacionInputPort consultarTiposVinculacionInputPort;

    public TipoVinculacionController(final ConsultarTiposVinculacionInputPort consultarTiposVinculacionInputPort) {
        this.consultarTiposVinculacionInputPort = Objects.requireNonNull(consultarTiposVinculacionInputPort,
                "El puerto de entrada ConsultarTiposVinculacionInputPort es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<List<TipoVinculacionDTO>> consultarTiposVinculacion() {
        return ResponseEntity.ok(consultarTiposVinculacionInputPort.execute());
    }
}
