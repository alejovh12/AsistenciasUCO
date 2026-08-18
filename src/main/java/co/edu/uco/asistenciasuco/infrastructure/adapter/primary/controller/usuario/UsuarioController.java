package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador primario REST para operaciones relacionadas con usuarios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public final class UsuarioController {

    private final CrearUsuarioInputPort crearUsuarioInputPort;

    public UsuarioController(final CrearUsuarioInputPort crearUsuarioInputPort) {
        if (ObjectHelper.isNull(crearUsuarioInputPort)) {
            throw new CrosscuttingException("El puerto de entrada CrearUsuarioInputPort es obligatorio.");
        }
        this.crearUsuarioInputPort = crearUsuarioInputPort;
    }

    @PostMapping
    public ResponseEntity<CrearUsuarioResultadoDTO> crearUsuario(@RequestBody final CrearUsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearUsuarioInputPort.execute(dto));
    }
}
