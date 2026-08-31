package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoidentificacion;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador primario REST para consulta del catalogo de tipos de identificacion.
 */
@RestController
@RequestMapping("/api/v1/tipos-identificacion")
public final class TipoIdentificacionController {

    private final ConsultarTiposIdentificacionInputPort consultarTiposIdentificacionInputPort;

    public TipoIdentificacionController(
            final ConsultarTiposIdentificacionInputPort consultarTiposIdentificacionInputPort
    ) {
        this.consultarTiposIdentificacionInputPort = Objects.requireNonNull(consultarTiposIdentificacionInputPort, "El puerto de entrada ConsultarTiposIdentificacionInputPort es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<List<TipoIdentificacionDTO>> consultarTiposIdentificacion() {
        return ResponseEntity.ok(consultarTiposIdentificacionInputPort.execute());
    }
}