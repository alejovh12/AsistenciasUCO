package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Retencion, ausentismo, comparativa de programas y aforo de grupos de la facultad del decano autenticado (HU102, HU103, HU106, HU144). */
@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoIndicadoresController {

    private final ConsultarKpisDecanoInputPort kpis;
    private final ConsultarComparativaProgramasInputPort comparativa;
    private final ConsultarAforoGruposInputPort aforo;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoIndicadoresController(final ConsultarKpisDecanoInputPort kpis, final ConsultarComparativaProgramasInputPort comparativa,
                                       final ConsultarAforoGruposInputPort aforo, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.kpis = Objects.requireNonNull(kpis, "ConsultarKpisDecanoInputPort es obligatorio.");
        this.comparativa = Objects.requireNonNull(comparativa, "ConsultarComparativaProgramasInputPort es obligatorio.");
        this.aforo = Objects.requireNonNull(aforo, "ConsultarAforoGruposInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/indicadores")
    public ResponseEntity<ApiDataResponse<KpisDecanoDTO>> consultarKpis(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                kpis.execute(new ConsultarKpisDecanoDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico))));
    }

    @GetMapping("/comparativa-programas")
    public ResponseEntity<ApiDataResponse<List<ComparativaProgramaDTO>>> consultarComparativa(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                comparativa.execute(new ConsultarComparativaProgramasDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico))));
    }

    @GetMapping("/aforo-grupos")
    public ResponseEntity<ApiListResponse<AforoGrupoDTO>> consultarAforo(
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico,
            @RequestParam(name = "soloHabilitados", required = false) final Boolean soloHabilitados
    ) {
        final AforoGruposPaginaDTO resultado = aforo.execute(new ConsultarAforoGruposDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico, idPrograma, soloHabilitados, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.grupos(), resultado.totalRegistros()));
    }
}
