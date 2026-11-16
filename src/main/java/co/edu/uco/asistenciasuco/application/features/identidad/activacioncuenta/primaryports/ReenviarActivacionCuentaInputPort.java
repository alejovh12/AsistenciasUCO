package co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.primaryports;

import java.util.UUID;

/**
 * IDENT-002: reenvio del enlace de activacion de una cuenta registrada por otra persona. El actor solo alcanza las cuentas
 * que puede gestionar (administrador: cualquiera; decano: su facultad; coordinador: su programa).
 */
public interface ReenviarActivacionCuentaInputPort {

    /** @return el mensaje institucional de exito ({@code ACT_001}). */
    String reenviar(UUID actorUsuario, String correo);
}
