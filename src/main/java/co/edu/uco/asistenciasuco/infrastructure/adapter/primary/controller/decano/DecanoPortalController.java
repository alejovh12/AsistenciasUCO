package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CrearCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoPortalController {

    private final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort;
    private final CrearCoordinadorInputPort crearCoordinadorInputPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DecanoPortalController(
            final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort,
            final CrearCoordinadorInputPort crearCoordinadorInputPort,
            final AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.consultarCoordinadoresInputPort = consultarCoordinadoresInputPort;
        this.crearCoordinadorInputPort = crearCoordinadorInputPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/coordinadores")
    public ResponseEntity<ApiListResponse<CoordinadorDTO>> consultarCoordinadores() {
        final var data = consultarCoordinadoresInputPort.execute(authenticatedUserProvider.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PatchMapping("/coordinadores/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleCoordinador(@PathVariable final UUID id) {
        throw unavailable("cambiar estado de coordinador");
    }

    @PostMapping("/coordinadores")
    public ResponseEntity<ApiDataResponse<Void>> crearCoordinador(@RequestBody final CrearCoordinadorRequest request) {
        crearCoordinadorInputPort.execute(new CrearCoordinadorDTO(
                request == null ? null : request.getNumeroIdentificacion(),
                request == null ? null : request.getPrimerNombre(),
                request == null ? null : request.getSegundoNombre(),
                request == null ? null : request.getPrimerApellido(),
                request == null ? null : request.getSegundoApellido(),
                request == null ? null : request.getCorreo(),
                request == null ? null : request.getIdPrograma(),
                request == null ? null : request.getPassword(),
                authenticatedUserProvider.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion de decano no esta disponible: " + operation + ".");
    }
}
