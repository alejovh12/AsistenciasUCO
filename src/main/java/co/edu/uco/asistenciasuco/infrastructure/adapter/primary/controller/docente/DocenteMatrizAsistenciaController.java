package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.primaryports.ConsultarMatrizAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.result.MatrizAsistenciaGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.primaryports.ConsultarResumenSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.result.ResumenSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CIERRE-W3M: matriz de asistencia del grupo (estudiantes x sesiones, paginada por estudiante y con busqueda) y resumen de sus
 * sesiones, calculados por la base de datos para el docente titular. Rutas nuevas bajo {@code /api/v1/docente/**}: el Golden Path
 * no cambia. La titularidad la validan Application y el SP (grupo ajeno: 403 {@code SEC_002}). Sin busqueda, {@code tamano} es 50.
 */
@RestController
@RequestMapping("/api/v1/docente/grupos/{grupoId}")
public final class DocenteMatrizAsistenciaController {

    private static final int TAMANO_POR_DEFECTO = 50;

    private final ConsultarMatrizAsistenciaGrupoInputPort consultarMatriz;
    private final ConsultarResumenSesionesGrupoInputPort consultarResumenSesiones;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteMatrizAsistenciaController(
            final ConsultarMatrizAsistenciaGrupoInputPort consultarMatriz,
            final ConsultarResumenSesionesGrupoInputPort consultarResumenSesiones,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarMatriz = Objects.requireNonNull(consultarMatriz, "ConsultarMatrizAsistenciaGrupoInputPort es obligatorio.");
        this.consultarResumenSesiones = Objects.requireNonNull(consultarResumenSesiones,
                "ConsultarResumenSesionesGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/matriz-asistencia")
    public ResponseEntity<MatrizAsistenciaResponse> consultarMatriz(
            @PathVariable final UUID grupoId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "busqueda", required = false) final String busqueda
    ) {
        final PaginacionRequestValidator.Pagina solicitada = PaginacionRequestValidator.resolver(pagina, tamano == null ? TAMANO_POR_DEFECTO : tamano);
        RequestValidationGuard.validate(BusquedaRequestValidator.validate(busqueda));
        return ResponseEntity.ok(MatrizAsistenciaResponse.from(consultarMatriz.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(
                grupoId, solicitada.numero(), solicitada.tamano(), busqueda, authenticatedUserResolver.requireAuthenticatedUserId()))));
    }

    @GetMapping("/resumen-sesiones")
    public ResponseEntity<ApiDataResponse<ResumenSesionesGrupoDTO>> consultarResumenSesiones(@PathVariable final UUID grupoId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, consultarResumenSesiones.execute(
                new ConsultarResumenSesionesGrupoInputPort.Consulta(grupoId, authenticatedUserResolver.requireAuthenticatedUserId()))));
    }

    /**
     * Pagina de la matriz: {@code sesiones} es el encabezado (orden por numero) y cada elemento trae una celda por sesion.
     * {@code totalElementos} cuenta estudiantes que cumplen la busqueda.
     */
    public record MatrizAsistenciaResponse(boolean exitoso, List<MatrizAsistenciaGrupoDTO.Sesion> sesiones,
                                           List<MatrizAsistenciaGrupoDTO.Fila> elementos, int numeroPagina, int tamanoPagina,
                                           long totalElementos, int totalPaginas) {

        static MatrizAsistenciaResponse from(final MatrizAsistenciaGrupoDTO dto) {
            final int paginas = dto.tamanoPagina() <= 0 || dto.totalElementos() <= 0
                    ? 0 : (int) ((dto.totalElementos() + dto.tamanoPagina() - 1) / dto.tamanoPagina());
            return new MatrizAsistenciaResponse(true, dto.sesiones(), dto.filas(), dto.numeroPagina(), dto.tamanoPagina(),
                    dto.totalElementos(), paginas);
        }
    }
}
