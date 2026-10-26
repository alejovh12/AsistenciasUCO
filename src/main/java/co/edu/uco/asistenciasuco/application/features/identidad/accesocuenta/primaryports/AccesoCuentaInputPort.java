package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports;

import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoCuentaDTO;

import java.util.UUID;

/**
 * Estado de acceso de las cuentas que un actor autorizado registró o gestiona, y su reconciliación idempotente.
 * El actor solo ve y reconcilia los roles dentro de su alcance (un coordinador nunca toca DECANO ni ADMINISTRADOR).
 */
public interface AccesoCuentaInputPort {

    AccesoCuentaDTO consultar(UUID actorUsuario, String correo);

    AccesoCuentaDTO reconciliar(UUID actorUsuario, String correo);
}
