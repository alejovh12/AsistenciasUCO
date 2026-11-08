package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.configuracion;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.ConsultarConfiguracionReclamosInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * CULM-001 D.2 (CONTRACT D 3.3, HU015): configuracion del formulario de reclamos (tope de soportes, tamano y tipos de archivo, limite
 * de subidas por minuto y dias del plazo) para ESTUDIANTE y DOCENTE. Los valores salen de la base de datos; el cliente no los duplica.
 */
@RestController
@RequestMapping("/api/v1/configuracion")
public final class ConfiguracionReclamosController {

    private final ConsultarConfiguracionReclamosInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ConfiguracionReclamosController(
            final ConsultarConfiguracionReclamosInputPort inputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultarConfiguracionReclamosInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/reclamos")
    public ResponseEntity<ApiDataResponse<ConfiguracionReclamosDTO>> consultarConfiguracionReclamos() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId())));
    }
}
