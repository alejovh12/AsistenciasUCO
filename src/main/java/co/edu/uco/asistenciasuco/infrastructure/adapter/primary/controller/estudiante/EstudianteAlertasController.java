package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.AlertaInasistenciaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.support.RevisionCancelacionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** CULM-001 C.1 (HU026C/HU013): alertas de inasistencia del estudiante autenticado (movil primero). Solo lectura. */
@RestController
@RequestMapping("/api/v1/estudiante/alertas")
public final class EstudianteAlertasController {

    private final ConsultarAlertasEstudianteInputPort consultarAlertas;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteAlertasController(
            final ConsultarAlertasEstudianteInputPort consultarAlertas,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarAlertas = consultarAlertas;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<AlertaInasistenciaResponse>> consultar(
            @RequestParam(name = "soloVigentes", defaultValue = "false") final boolean soloVigentes,
            @RequestParam(name = "soloNoLeidas", defaultValue = "false") final boolean soloNoLeidas,
            @RequestParam(name = "grupoId", required = false) final UUID grupoId,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final PaginaAlertas resultado = consultarAlertas.execute(new ConsultarAlertasEstudianteInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), pagina, tamano, soloNoLeidas, soloVigentes, grupoId));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.filas().stream().map(RevisionCancelacionHttpMapper::toResponse).toList(),
                pagina, tamano, resultado.total()));
    }
}
