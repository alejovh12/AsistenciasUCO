package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo;

import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.ConsultarConfiguracionRegistroEstudianteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.response.ConfiguracionRegistroEstudianteResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * IDENT-001 D3: entrega al frontend el dominio institucional vigente del correo del estudiante, para validar el
 * formulario de registro manual sin escribir el dominio en el cliente.
 */
@RestController
@RequestMapping("/api/v1/configuracion/registro-estudiante")
public final class ConfiguracionRegistroEstudianteController {

    private final ConsultarConfiguracionRegistroEstudianteInputPort inputPort;

    public ConfiguracionRegistroEstudianteController(final ConsultarConfiguracionRegistroEstudianteInputPort inputPort) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultarConfiguracionRegistroEstudianteInputPort es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiDataResponse<ConfiguracionRegistroEstudianteResponse>> consultar() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, new ConfiguracionRegistroEstudianteResponse(inputPort.execute())));
    }
}
