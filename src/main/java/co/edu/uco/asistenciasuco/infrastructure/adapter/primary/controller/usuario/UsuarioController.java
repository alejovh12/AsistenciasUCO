package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.ProvisionarUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper.CrearUsuarioHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response.CrearUsuarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation.CrearUsuarioRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private final ProvisionarUsuarioInputPort provisionarUsuarioInputPort;

    public UsuarioController(final ProvisionarUsuarioInputPort provisionarUsuarioInputPort) {
        this.provisionarUsuarioInputPort = Objects.requireNonNull(
                provisionarUsuarioInputPort,
                "ProvisionarUsuarioInputPort es obligatorio."
        );
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiDataResponse<Void>> consultarPerfilUsuarioAutenticado() {
        throw new FeatureUnavailableException(
                "La consulta de perfil requiere una vertical de lectura sobre vistas publicas de usuario."
        );
    }

    @PutMapping("/perfil")
    public ResponseEntity<ApiDataResponse<Void>> actualizarPerfilUsuarioAutenticado(@RequestBody final Object request) {
        throw new FeatureUnavailableException(
                "La actualizacion de perfil requiere un command publico de DB; no se permite UPDATE directo."
        );
    }

    @PostMapping
    @AuditableOperation(
            action = "CREAR_USUARIO",
            resourceType = "USUARIO"
    )
    public ResponseEntity<CrearUsuarioResponse> crearUsuario(@RequestBody final CrearUsuarioRequest request) {
        RequestValidationGuard.validate(CREATE_USER_VALIDATOR.validate(request));
        final ProvisionarUsuarioDTO dto = CrearUsuarioHttpMapper.toApplicationDTO(request);
        final ProvisionarUsuarioResultadoDTO resultado = provisionarUsuarioInputPort.execute(dto);
        if (!ObjectHelper.isNull(resultado.usuarioId())) {
            AuditRequestAttributes.storeResourceId(resultado.usuarioId().toString());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CrearUsuarioResponse.from(resultado));
    }
}
