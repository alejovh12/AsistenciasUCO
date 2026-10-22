package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoprograma;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogos de solo lectura para el formulario de programa: tipos, modalidades y jornadas activos. */
@RestController
@RequestMapping("/api/v1")
public final class CatalogoProgramaController {

    private final ConsultarCatalogosProgramaInputPort inputPort;

    public CatalogoProgramaController(final ConsultarCatalogosProgramaInputPort inputPort) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultarCatalogosProgramaInputPort es obligatorio.");
    }

    @GetMapping("/tipos-programa")
    public ResponseEntity<List<CatalogoProgramaDTO>> tiposPrograma() {
        return ResponseEntity.ok(inputPort.tiposPrograma());
    }

    @GetMapping("/modalidades-programa")
    public ResponseEntity<List<CatalogoProgramaDTO>> modalidades() {
        return ResponseEntity.ok(inputPort.modalidades());
    }

    @GetMapping("/jornadas-programa")
    public ResponseEntity<List<CatalogoProgramaDTO>> jornadas() {
        return ResponseEntity.ok(inputPort.jornadas());
    }
}
