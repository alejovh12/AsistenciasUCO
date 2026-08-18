package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para crear un usuario base.
 */
public interface CrearUsuarioInputPort extends InteractorWithReturn<CrearUsuarioDTO, CrearUsuarioResultadoDTO> {
}
