package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import java.util.UUID;

/** Puerto interno del modulo de correo: registra el SHA-256 de un token de verificacion recien emitido. */
@FunctionalInterface
public interface TokenVerificacionCorreoRepository {

    /** Revoca el token activo previo del usuario y registra el nuevo hash con la vigencia configurada. */
    void registrar(UUID idUsuario, byte[] tokenHash);
}
