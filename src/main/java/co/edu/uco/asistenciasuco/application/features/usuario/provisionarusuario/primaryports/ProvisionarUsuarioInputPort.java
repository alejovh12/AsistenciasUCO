package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;

public interface ProvisionarUsuarioInputPort {

    ProvisionarUsuarioResultadoDTO execute(ProvisionarUsuarioDTO dto);
}
