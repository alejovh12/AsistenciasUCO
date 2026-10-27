package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** KPIs de cumplimiento y permanencia y alerta temprana del programa del coordinador autenticado (HU065, HU066, HU070). */
@RestController
@RequestMapping("/api/v1/coordinador")
public final class CoordinadorIndicadoresController {

    private final ConsultarKpisCoordinadorInputPort kpis;
    private final ConsultarAlertaTempranaInputPort alertas;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorIndicadoresController(final ConsultarKpisCoordinadorInputPort kpis, final ConsultarAlertaTempranaInputPort alertas,
                                            final AuthenticatedUserResolver authenticatedUserResolver) {
        this.kpis = Objects.requireNonNull(kpis, "ConsultarKpisCoordinadorInputPort es obligatorio.");
        this.alertas = Objects.requireNonNull(alertas, "ConsultarAlertaTempranaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/indicadores")
    public ResponseEntity<ApiDataResponse<KpisCoordinadorDTO>> consultarKpis(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                kpis.execute(new ConsultarKpisCoordinadorDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico))));
    }

    @GetMapping("/alertas-tempranas")
    public ResponseEntity<ApiListResponse<AlertaTempranaDTO>> consultarAlertas(
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "semaforo", required = false) final String semaforo,
            @RequestParam(name = "nombre", required = false) final String nombre,
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        final AlertasPaginaDTO resultado = alertas.execute(new ConsultarAlertaTempranaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico, semaforo, nombre, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.alertas(), resultado.totalRegistros()));
    }
}
