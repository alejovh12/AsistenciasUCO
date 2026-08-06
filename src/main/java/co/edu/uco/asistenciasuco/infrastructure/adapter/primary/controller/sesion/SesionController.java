package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador primario REST para operaciones relacionadas con sesiones.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
public final class SesionController {

    private final CrearSesionInputPort crearSesionInputPort;
    private final ConsultarSesionInputPort consultarSesionInputPort;
    private final CerrarSesionInputPort cerrarSesionInputPort;

    public SesionController(
            final CrearSesionInputPort crearSesionInputPort,
            final ConsultarSesionInputPort consultarSesionInputPort,
            final CerrarSesionInputPort cerrarSesionInputPort
    ) {
        if (ObjectHelper.isNull(crearSesionInputPort)) {
            throw new CrosscuttingException("El puerto de entrada CrearSesionInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarSesionInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarSesionInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(cerrarSesionInputPort)) {
            throw new CrosscuttingException("El puerto de entrada CerrarSesionInputPort es obligatorio.");
        }

        this.crearSesionInputPort = crearSesionInputPort;
        this.consultarSesionInputPort = consultarSesionInputPort;
        this.cerrarSesionInputPort = cerrarSesionInputPort;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearSesion(@RequestBody final CrearSesionDTO dto) {
        crearSesionInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("mensaje", "Sesion creada correctamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultas")
    public ResponseEntity<Map<String, Object>> consultarSesion(@RequestBody final ConsultarSesionDTO dto) {
        final SesionConsultadaDTO sesion = consultarSesionInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("datos", sesion);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cierres")
    public ResponseEntity<Map<String, Object>> cerrarSesion(@RequestBody final CerrarSesionDTO dto) {
        cerrarSesionInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("mensaje", "Sesion cerrada correctamente.");

        return ResponseEntity.ok(response);
    }
}
