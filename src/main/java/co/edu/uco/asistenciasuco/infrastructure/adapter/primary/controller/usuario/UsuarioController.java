package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response.CrearUsuarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper.CrearUsuarioHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation.CrearUsuarioRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;

/**
 * Adaptador primario REST para operaciones relacionadas con usuarios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public final class UsuarioController {

    private static final CrearUsuarioRequestValidator CREATE_USER_VALIDATOR = new CrearUsuarioRequestValidator();

    private final CrearUsuarioInputPort crearUsuarioInputPort;

    public UsuarioController(final CrearUsuarioInputPort crearUsuarioInputPort) {
        this.crearUsuarioInputPort = Objects.requireNonNull(crearUsuarioInputPort, "El puerto de entrada CrearUsuarioInputPort es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(
            action = "CREAR_USUARIO",
            resourceType = "USUARIO"
    )
    public ResponseEntity<CrearUsuarioResponse> crearUsuario(@RequestBody final CrearUsuarioRequest request) {
        RequestValidationGuard.validate(CREATE_USER_VALIDATOR.validate(request));
        final CrearUsuarioDTO dto = CrearUsuarioHttpMapper.toApplicationDTO(request);
        final CrearUsuarioResultadoDTO resultado = crearUsuarioInputPort.execute(dto);
        if (!ObjectHelper.isNull(resultado.getUsuarioId())) {
            AuditRequestAttributes.storeResourceId(resultado.getUsuarioId().toString());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CrearUsuarioResponse.from(resultado));
    }
}
