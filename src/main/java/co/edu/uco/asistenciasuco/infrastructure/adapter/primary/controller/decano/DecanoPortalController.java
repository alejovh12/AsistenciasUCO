package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CrearCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal del decano (F-DEC-004: paginacion en servidor con ApiPageResponse en /coordinadores).
 */
@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoPortalController {

    private final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort;
    private final CrearCoordinadorInputPort crearCoordinadorInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoPortalController(
            final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort,
            final CrearCoordinadorInputPort crearCoordinadorInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarCoordinadoresInputPort = consultarCoordinadoresInputPort;
        this.crearCoordinadorInputPort = crearCoordinadorInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/coordinadores")
    public ResponseEntity<ApiPageResponse<CoordinadorDTO>> consultarCoordinadores(
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final int pag = ConsultaPaginadaRules.pagina(pagina);
        final int tam = ConsultaPaginadaRules.tamano(tamano);
        final var resultado = consultarCoordinadoresInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(), pag, tam);
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos(),
                resultado.numeroPagina(),
                resultado.tamanoPagina(),
                resultado.totalElementos()
        ));
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
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
