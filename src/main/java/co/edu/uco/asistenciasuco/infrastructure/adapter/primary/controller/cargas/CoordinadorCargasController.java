package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.exception.business.TooManyRequestsException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coordinador/cargas")
public class CoordinadorCargasController {

    private static final int LIMITE_CARGAS_POR_MINUTO = 3;

    private final ValidarCargaUsuariosInputPort validarInputPort;
    private final EjecutarCargaUsuariosInputPort ejecutarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final SlidingWindowRateLimiter rateLimiter;

    public CoordinadorCargasController(
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
        final byte[] bytes = PlantillasCargaMasiva.plantillaDocentes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-carga-docentes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping("/validacion")
    public ResponseEntity<ApiDataResponse<ResultadoValidacionCargaDTO>> validar(
            @RequestParam("archivo") final MultipartFile archivo
    ) {
        final UUID actorId = requireActorConRateLimit();
        final byte[] bytes = extraerBytes(archivo);
        final String nombreArchivo = archivo != null ? archivo.getOriginalFilename() : "docentes.csv";
        final ValidarCargaUsuariosCommand command = new ValidarCargaUsuariosCommand(
                actorId, PerfilDestinoCarga.DOCENTE, nombreArchivo, bytes
        );
        final ResultadoValidacionCargaDTO resultado = validarInputPort.validar(command);
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    @PostMapping
    @AuditableOperation(action = "CARGA_MASIVA_DOCENTE", resourceType = "CARGA_MASIVA", metadataResponseFields = {"totalFilas", "creadas", "yaExistentes", "conError"})
    public ResponseEntity<ApiDataResponse<ResultadoCargaDTO>> ejecutar(
            @RequestParam("archivo") final MultipartFile archivo,
            @RequestParam("huella") final String huella
    ) {
        final UUID actorId = requireActorConRateLimit();
        final byte[] bytes = extraerBytes(archivo);
        final String nombreArchivo = archivo != null ? archivo.getOriginalFilename() : "docentes.csv";
        final EjecutarCargaUsuariosCommand command = new EjecutarCargaUsuariosCommand(
                actorId, PerfilDestinoCarga.DOCENTE, nombreArchivo, bytes, huella
        );
        final ResultadoCargaDTO resultado = ejecutarInputPort.ejecutar(command);
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    private UUID requireActorConRateLimit() {
        final UUID actorId = authenticatedUserResolver.requireAuthenticatedUserId();
        if (!rateLimiter.tryAcquire(actorId.toString(), LIMITE_CARGAS_POR_MINUTO)) {
            throw new TooManyRequestsException(CargaMasivaErrorCode.CMA_014);
        }
        return actorId;
    }

    private static byte[] extraerBytes(final MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_013);
        }
        try {
            return archivo.getBytes();
        } catch (final IOException e) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_013);
        }
    }
}
