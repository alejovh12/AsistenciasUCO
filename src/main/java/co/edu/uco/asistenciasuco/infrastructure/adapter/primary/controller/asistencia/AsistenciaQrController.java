package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.RegistrarAsistenciaAutonomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaQrRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
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
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AsistenciaQrController(
            final RegistrarAsistenciaAutonomaInputPort registrarAsistenciaAutonomaInputPort,
            final AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.registrarAsistenciaAutonomaInputPort = Objects.requireNonNull(
                registrarAsistenciaAutonomaInputPort,
                "RegistrarAsistenciaAutonomaInputPort es obligatorio."
        );
        this.authenticatedUserProvider = Objects.requireNonNull(
                authenticatedUserProvider,
                "AuthenticatedUserProvider es obligatorio."
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
                authenticatedUserProvider.requireAuthenticatedUserId()
        );
        registrarAsistenciaAutonomaInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
