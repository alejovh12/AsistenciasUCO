package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/cargas")
public class AdminCargasController {

    private final ValidarCargaUsuariosInputPort validarInputPort;
    private final EjecutarCargaUsuariosInputPort ejecutarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final SlidingWindowRateLimiter rateLimiter;

    public AdminCargasController(
            final ValidarCargaUsuariosInputPort validarInputPort,
            final EjecutarCargaUsuariosInputPort ejecutarInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver,
            final SlidingWindowRateLimiter rateLimiter
    ) {
        this.validarInputPort = Objects.requireNonNull(validarInputPort, "ValidarCargaUsuariosInputPort es obligatorio.");
        this.ejecutarInputPort = Objects.requireNonNull(ejecutarInputPort, "EjecutarCargaUsuariosInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "SlidingWindowRateLimiter es obligatorio.");
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantilla() {
        throw new UnsupportedOperationException("STUB RED");
    }

    @PostMapping("/validacion")
    public ResponseEntity<ApiDataResponse<ResultadoValidacionCargaDTO>> validar(
            @RequestParam("archivo") final MultipartFile archivo
    ) {
        throw new UnsupportedOperationException("STUB RED");
    }

    @PostMapping
    @AuditableOperation(action = "CARGA_MASIVA_DECANO", resourceType = "CARGA_MASIVA", metadataResponseFields = {"totalFilas", "creadas", "yaExistentes", "conError"})
    public ResponseEntity<ApiDataResponse<ResultadoCargaDTO>> ejecutar(
            @RequestParam("archivo") final MultipartFile archivo,
            @RequestParam("huella") final String huella
    ) {
        throw new UnsupportedOperationException("STUB RED");
    }
}
