package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * IDENT-001: persistencia de la verificacion de correo. Application maneja el token como un valor opaco: el hash que se
 * compara en la base de datos lo calcula el adaptador, y la base de datos es la autoridad sobre vigencia y un solo uso.
 */
public interface VerificacionCorreoRepositoryPort {

    /**
     * Devuelve el usuario dueno de un token vigente <b>sin consumirlo</b>. Para cualquier token invalido (inexistente,
     * vencido, usado, revocado, de usuario inactivo o con correo ya verificado) devuelve vacio.
     */
    Optional<UUID> consultarUsuarioPorToken(String token);

    /**
     * Consume el token (un solo uso) y marca el correo del usuario como verificado en una unica transaccion.
     *
     * @throws co.edu.uco.asistenciasuco.application.exception.validation.ValidationException si el token ya no es valido.
     */
    void confirmar(String token);

    /**
     * Valida que el ejecutor pueda reenviar el enlace al estudiante (docente titular del grupo o coordinador de su
     * programa), que el correo siga sin verificar y que haya pasado la espera minima.
     *
     * @return el usuario del estudiante al que se le registrara el aviso.
     */
    UUID consultarReenvio(UUID grupoId, UUID estudianteId, UUID actorUsuarioId);
}
