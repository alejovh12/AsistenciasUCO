package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import co.edu.uco.notificaciones.application.model.VistaPrevia;
import co.edu.uco.notificaciones.application.port.in.PlantillasUseCase;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.NuevaVersionRequest;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.VistaPreviaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Plantillas: listado con descripcion y marcadores permitidos, versionado, activacion y vista previa. */
@RestController
@RequestMapping("/api/v1/plantillas")
public class PlantillasController {

    private final PlantillasUseCase plantillas;

    public PlantillasController(final PlantillasUseCase plantillas) {
        this.plantillas = plantillas;
    }

    @GetMapping
    public List<TipoPlantillaAdmin> consultar(@RequestParam(required = false) final String idioma) {
        return plantillas.consultar(idioma);
    }

    @PostMapping("/{tipo}/versiones")
    public ResponseEntity<VersionPlantilla> crearVersion(@PathVariable final String tipo, @Valid @RequestBody final NuevaVersionRequest cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantillas.crearVersion(tipo, cuerpo.idioma(), cuerpo.asunto(), cuerpo.cuerpo(), cuerpo.creadoPor()));
    }

    @PostMapping("/{tipo}/versiones/{version}/activacion")
    public VersionPlantilla activar(@PathVariable final String tipo, @PathVariable final int version,
                                    @RequestParam(required = false) final String idioma) {
        return plantillas.activarVersion(tipo, idioma, version);
    }

    @PostMapping("/vista-previa")
    public VistaPrevia vistaPrevia(@Valid @RequestBody final VistaPreviaRequest cuerpo) {
        return plantillas.vistaPrevia(cuerpo.tipo(), cuerpo.idioma(), cuerpo.asunto(), cuerpo.cuerpo());
    }
}
