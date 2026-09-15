package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.RegistrarAsistenciaAutonomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaQrRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.Objects;

/**
 * Entrada HTTP para QR/PIN de asistencia.
 *
 * <p>La DB es la unica fuente de verdad del codigo de asistencia. No se crean tokens
 * en memoria ni codigos alternos desde el backend.</p>
 */
@RestController
@RequestMapping("/api/v1")
public final class AsistenciaQrController {

    private final RegistrarAsistenciaAutonomaInputPort registrarAsistenciaAutonomaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AsistenciaQrController(
            final RegistrarAsistenciaAutonomaInputPort registrarAsistenciaAutonomaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.registrarAsistenciaAutonomaInputPort = Objects.requireNonNull(
                registrarAsistenciaAutonomaInputPort,
                "RegistrarAsistenciaAutonomaInputPort es obligatorio."
        );
        this.authenticatedUserResolver = Objects.requireNonNull(
                authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio."
        );
    }

    @GetMapping("/sesiones/{sesionId}/qr-token")
    public ResponseEntity<ApiDataResponse<Void>> generarQrToken(@PathVariable final UUID sesionId) {
        throw new FeatureUnavailableException(
                "La presentacion QR/PIN requiere una query vertical de sesion para exponer el codigo administrado por DB."
        );
    }

    @PostMapping("/estudiante/asistencia-qr")
    public ResponseEntity<ApiDataResponse<Void>> registrarAsistenciaPorQr(
            @RequestBody final RegistrarAsistenciaQrRequest request
    ) {
        final RegistrarAsistenciaAutonomaDTO dto = AsistenciaHttpMapper.toApplicationDTO(
                request,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        registrarAsistenciaAutonomaInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
