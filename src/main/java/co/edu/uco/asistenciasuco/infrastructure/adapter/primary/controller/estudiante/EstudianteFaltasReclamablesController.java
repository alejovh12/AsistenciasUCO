package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Resultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.FaltaReclamableResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * ATA D10 (HU026B): tomas en falta que el estudiante autenticado aun puede reclamar. Solo lectura. El perfil lo fuerza la DB: 403 si el usuario
 * no es estudiante ({@code SEC_001}), 400 si la pagina no es valida ({@code PAG_001}). Los parametros de pagina son {@code numeroPagina} y
 * {@code tamanoPagina} (addendum J.2); se aceptan tambien {@code pagina} y {@code tamano}, los nombres del resto de listados del estudiante.
 */
@RestController
@RequestMapping("/api/v1/estudiante/faltas-reclamables")
public final class EstudianteFaltasReclamablesController {

    private final ConsultarFaltasReclamablesInputPort consultar;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteFaltasReclamablesController(
            final ConsultarFaltasReclamablesInputPort consultar,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultar = Objects.requireNonNull(consultar, "ConsultarFaltasReclamablesInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<FaltaReclamableResponse>> consultar(
            @RequestParam(name = "numeroPagina", required = false) final Integer numeroPagina,
            @RequestParam(name = "tamanoPagina", required = false) final Integer tamanoPagina,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final Resultado resultado = consultar.execute(new ConsultarFaltasReclamablesInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                numeroPagina != null ? numeroPagina : pagina,
                tamanoPagina != null ? tamanoPagina : tamano));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(FaltaReclamableResponse::from).toList(),
                resultado.pagina(), resultado.tamano(), resultado.total()));
    }
}
