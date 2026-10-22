package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.ConsultarGruposDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Grupos de la facultad del decano autenticado. Controlador propio (no toca {@link DecanoPortalController}) y cubierto
 * por el comodin {@code /api/v1/decano/**} de {@code SecurityConfig}; el ambito de facultad lo fuerza el caso de uso.
 */
@RestController
@RequestMapping("/api/v1/decano/grupos")
public final class DecanoGrupoController {

    private final ConsultarGruposDecanoInputPort consultarGruposDecanoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoGrupoController(
            final ConsultarGruposDecanoInputPort consultarGruposDecanoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarGruposDecanoInputPort = consultarGruposDecanoInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<GrupoDTO>> listarGruposDeLaFacultad() {
        final List<GrupoDTO> grupos = consultarGruposDecanoInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, grupos, grupos.size()));
    }
}
