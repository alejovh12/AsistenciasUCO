package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.ConsultarOfertaGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.ConsultarOfertaGruposDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.GrupoOfertaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.OfertaGruposPaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** HU008: oferta de grupos (con cupos) de los programas del estudiante autenticado. */
@RestController
@RequestMapping("/api/v1/estudiante/oferta-academica")
public final class EstudianteOfertaAcademicaController {

    private final ConsultarOfertaGruposInputPort consultarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteOfertaAcademicaController(
            final ConsultarOfertaGruposInputPort consultarInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort, "ConsultarOfertaGruposInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<GrupoOfertaDTO>> consultar(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final OfertaGruposPaginaDTO resultado = consultarInputPort.execute(new ConsultarOfertaGruposDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), busqueda, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.grupos(), (int) resultado.total()));
    }
}
