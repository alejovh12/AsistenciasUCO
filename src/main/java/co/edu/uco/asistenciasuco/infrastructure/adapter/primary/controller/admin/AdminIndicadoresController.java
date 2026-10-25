package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Asistencia global, cumplimiento, censo, comparativa de facultades y tendencia de ausentismo de la institucion del administrador autenticado (HU133-HU139). */
@RestController
@RequestMapping("/api/v1/admin")
public final class AdminIndicadoresController {

    private final ConsultarKpisInstitucionInputPort kpis;
    private final ConsultarComparativaFacultadesInputPort comparativa;
    private final ConsultarTendenciaAusentismoInputPort tendencia;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminIndicadoresController(final ConsultarKpisInstitucionInputPort kpis, final ConsultarComparativaFacultadesInputPort comparativa,
                                      final ConsultarTendenciaAusentismoInputPort tendencia, final AuthenticatedUserResolver authenticatedUserResolver) {
        this.kpis = Objects.requireNonNull(kpis, "ConsultarKpisInstitucionInputPort es obligatorio.");
        this.comparativa = Objects.requireNonNull(comparativa, "ConsultarComparativaFacultadesInputPort es obligatorio.");
        this.tendencia = Objects.requireNonNull(tendencia, "ConsultarTendenciaAusentismoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/indicadores")
    public ResponseEntity<ApiDataResponse<KpisInstitucionDTO>> consultarKpis(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                kpis.execute(new ConsultarKpisInstitucionDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico))));
    }

    @GetMapping("/comparativa-facultades")
    public ResponseEntity<ApiDataResponse<List<ComparativaFacultadDTO>>> consultarComparativa(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                comparativa.execute(new ConsultarComparativaFacultadesDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico))));
    }

    @GetMapping("/tendencia-ausentismo")
    public ResponseEntity<ApiDataResponse<List<PuntoTendenciaDTO>>> consultarTendencia(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico,
            @RequestParam(name = "granularidad", required = false) final String granularidad
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                tendencia.execute(new ConsultarTendenciaAusentismoDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idPeriodoAcademico, granularidad))));
    }
}
