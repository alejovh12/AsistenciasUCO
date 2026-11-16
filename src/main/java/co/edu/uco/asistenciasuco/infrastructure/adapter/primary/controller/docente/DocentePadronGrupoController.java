package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.ConsultarPadronGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BusquedaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.PaginacionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * CIERRE-W1A: padron del grupo del docente titular, paginado y con busqueda en el servidor
 * ({@code GET /api/v1/docente/grupos/{grupoId}/estudiantes?pagina&tamano&busqueda}). Es una ruta propia del docente porque
 * {@code GET /api/v1/grupos/{grupoId}/estudiantes} esta congelada en el Golden Path (lista completa). La titularidad la validan
 * Application y el SP; un grupo ajeno o inexistente responde 403 (SEC_002). No pertenece al golden path.
 */
@RestController
@RequestMapping("/api/v1/docente/grupos")
public final class DocentePadronGrupoController {

    private final ConsultarPadronGrupoInputPort consultarPadronGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocentePadronGrupoController(
            final ConsultarPadronGrupoInputPort consultarPadronGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarPadronGrupoInputPort = Objects.requireNonNull(consultarPadronGrupoInputPort,
                "ConsultarPadronGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/{grupoId}/estudiantes")
    public ResponseEntity<ApiPageResponse<EstudianteGrupoDTO>> consultarPadron(
            @PathVariable final UUID grupoId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "busqueda", required = false) final String busqueda
    ) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano);
        RequestValidationGuard.validate(BusquedaRequestValidator.validate(busqueda));
        final PadronGrupoPaginaDTO resultado = consultarPadronGrupoInputPort.execute(new ConsultarPadronGrupoDTO(
                grupoId, authenticatedUserResolver.requireAuthenticatedUserId(), solicitada.numero(), solicitada.tamano(), busqueda));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.estudiantes(), resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalRegistros()));
    }
}
