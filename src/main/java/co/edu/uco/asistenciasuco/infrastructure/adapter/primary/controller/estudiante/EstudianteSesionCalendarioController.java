package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.ConsultarCalendarioEstudianteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.SesionCalendarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * ATA D7: sesiones del estudiante (grupos con matricula inscrita) entre dos fechas locales institucionales, para el horario semanal
 * con fechas. El perfil y el rango los valida la DB: 403 si no es estudiante ({@code SEC_001}), 400 {@code CAL_001} si falta una fecha,
 * hasta &lt; desde o el rango pasa de 31 dias.
 */
@RestController
@RequestMapping("/api/v1/estudiante/sesiones")
public final class EstudianteSesionCalendarioController {

    private final ConsultarCalendarioEstudianteInputPort consultar;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteSesionCalendarioController(
            final ConsultarCalendarioEstudianteInputPort consultar,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultar = Objects.requireNonNull(consultar, "ConsultarCalendarioEstudianteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/calendario")
    public ResponseEntity<ApiDataResponse<List<SesionCalendarioResponse>>> consultar(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate hasta
    ) {
        final List<SesionCalendarioResponse> datos = consultar.execute(new CalendarioSesiones.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), desde, hasta)).stream().map(SesionCalendarioResponse::from).toList();
        return ResponseEntity.ok(new ApiDataResponse<>(true, datos));
    }
}
