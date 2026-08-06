package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoidentificacion;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        if (ObjectHelper.isNull(consultarTiposIdentificacionInputPort)) {
            throw new CrosscuttingException(
                    "El puerto de entrada ConsultarTiposIdentificacionInputPort es obligatorio."
            );
        }
        this.consultarTiposIdentificacionInputPort = consultarTiposIdentificacionInputPort;
    }

    @GetMapping
    public ResponseEntity<List<TipoIdentificacionDTO>> consultarTiposIdentificacion() {
        return ResponseEntity.ok(consultarTiposIdentificacionInputPort.execute());
    }
}
