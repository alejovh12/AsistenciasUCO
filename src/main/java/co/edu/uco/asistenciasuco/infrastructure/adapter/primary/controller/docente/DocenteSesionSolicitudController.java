package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.primaryports.ConsultarSolicitudesSesionDocenteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response.SolicitudSesionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * ATA D6: lista de solo lectura de las solicitudes de sesion extraordinaria del docente titular, con el mismo contrato de fila que la
 * bandeja del coordinador. El rol y la titularidad los fuerza la DB: 403 si el usuario no es docente ({@code SEC_001}), 400 si la
 * pagina no es valida ({@code PAG_001}).
 */
@RestController
@RequestMapping("/api/v1/docente/sesiones")
public final class DocenteSesionSolicitudController {

    private final ConsultarSolicitudesSesionDocenteInputPort consultar;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteSesionSolicitudController(
            final ConsultarSolicitudesSesionDocenteInputPort consultar,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultar = Objects.requireNonNull(consultar, "ConsultarSolicitudesSesionDocenteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<ApiPageResponse<SolicitudSesionResponse>> consultar(
            @RequestParam(name = "soloPendientes", defaultValue = "true") final boolean soloPendientes,
            @RequestParam(name = "grupoId", required = false) final UUID grupoId,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final Resultado resultado = consultar.execute(new ConsultarSolicitudesSesionDocenteInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), soloPendientes, grupoId, pagina, tamano));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(SolicitudSesionResponse::from).toList(),
                resultado.pagina(), resultado.tamano(), resultado.total()));
    }
}
