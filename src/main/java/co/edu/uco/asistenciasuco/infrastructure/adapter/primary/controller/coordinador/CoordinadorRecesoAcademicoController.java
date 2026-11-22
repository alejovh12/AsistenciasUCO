package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.RecesoAcademicoPeriodoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DEC-081: consulta de los dias de receso academico de un periodo para el COORDINADOR y el DECANO (solo lectura; la regla
 * {@code GET /api/v1/coordinador/periodos-academicos/**} de SecurityConfig ya los admite). El ambito institucional lo aplica la DB.
 */
@RestController
@RequestMapping("/api/v1/coordinador/periodos-academicos/{idPeriodo}/recesos")
public final class CoordinadorRecesoAcademicoController {

    private final ConsultarRecesosPeriodoInputPort consultarRecesos;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorRecesoAcademicoController(
            final ConsultarRecesosPeriodoInputPort consultarRecesos,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarRecesos = Objects.requireNonNull(consultarRecesos, "ConsultarRecesosPeriodoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<RecesoAcademicoPeriodoResponse>> consultar(@PathVariable final UUID idPeriodo) {
        final List<RecesoAcademicoPeriodoResponse> recesos = consultarRecesos.execute(
                        new ConsultarRecesosPeriodoInputPort.Comando(idPeriodo, authenticatedUserResolver.requireAuthenticatedUserId()))
                .stream().map(RecesoAcademicoPeriodoResponse::from).toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, recesos, recesos.size()));
    }
}