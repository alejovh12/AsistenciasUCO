package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.mensajesinterfaz;

import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * INT-003 (MessageUcoLab 0.3.0, paso 12): textos de la interfaz para la SPA, sin sesion (tambien se muestran en el login).
 * Ruta publica declarada una por una en {@code SecurityConfig}, exenta del PEP y con limite de intentos por IP
 * ({@code MensajesInterfazRateLimitFilter}); el caso de uso solo responde codigos {@code UI###}.
 */
@RestController
@RequestMapping(MensajesInterfazPublicaController.RUTA_BASE)
public final class MensajesInterfazPublicaController {

    public static final String RUTA_BASE = "/api/v1/public/mensajes-interfaz";

    /** Texto de la interfaz; {@code contenido} puede traer marcadores {@code {nombre}} que llena la SPA. */
    public record MensajeInterfazResponse(String codigo, String contenido) {
    }

    private final ConsultarMensajeInterfazInputPort inputPort;

    public MensajesInterfazPublicaController(final ConsultarMensajeInterfazInputPort inputPort) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultarMensajeInterfazInputPort es obligatorio.");
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiDataResponse<MensajeInterfazResponse>> consultar(@PathVariable("codigo") final String codigo) {
        final var mensaje = inputPort.execute(codigo);
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MensajeInterfazResponse(mensaje.codigo(), mensaje.contenido())));
    }
}
