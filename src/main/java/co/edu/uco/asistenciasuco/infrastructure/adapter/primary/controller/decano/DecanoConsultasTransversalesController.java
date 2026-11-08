package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ProgramaCreditosDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.PaginacionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
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
 * HU112 (tabla unificada de programas y creditos), HU128 (docentes de la facultad por programa), HU143 (busqueda de
 * asignaturas de su facultad) y HU157 (historial del estudiante) del decano. Solo lectura; controlador propio cubierto
 * por el comodin {@code /api/v1/decano/**} de {@code SecurityConfig}. El SP exige el perfil y limita las filas a la
 * facultad del decano.
 */
@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoConsultasTransversalesController {

    private final ConsultasTransversalesInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoConsultasTransversalesController(final ConsultasTransversalesInputPort inputPort,
                                                  final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping("/programas-creditos")
    public ResponseEntity<ApiPageResponse<ProgramaCreditosDTO>> consultarProgramasCreditos(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano);
        return paginaDe(inputPort.consultarProgramasCreditos(usuario(), busqueda, estaActivo, pagina, tamano), solicitada);
    }

    @GetMapping("/docentes")
    public ResponseEntity<ApiPageResponse<DocenteFacultadDTO>> consultarDocentes(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano);
        return paginaDe(inputPort.consultarDocentes(usuario(), busqueda, idPrograma, estaActivo, pagina, tamano), solicitada);
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiPageResponse<AsignaturaInstitucionDTO>> buscarAsignaturas(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "estaActiva", required = false) final Boolean estaActiva,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano);
        return paginaDe(inputPort.buscarAsignaturas(usuario(), busqueda, null, estaActiva, pagina, tamano), solicitada);
    }

    @GetMapping("/estudiantes/{estudianteId}/historial")
    public ResponseEntity<ApiDataResponse<FichaUnificadaDTO>> consultarHistorial(
            @PathVariable("estudianteId") final UUID estudianteId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.consultarHistorial(usuario(), estudianteId)));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }

    private static <T> ResponseEntity<ApiPageResponse<T>> paginaDe(final PaginaDTO<T> pagina,
                                                                   final PaginacionRequestValidator.Pagina solicitada) {
        return ResponseEntity.ok(ApiPageResponse.of(pagina.elementos(), solicitada.numero(), solicitada.tamano(), pagina.total()));
    }
}
