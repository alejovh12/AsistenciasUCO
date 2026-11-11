package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * HU177: persistencia del enlace de un solo uso para definir una contrasena nueva. Application maneja el token como un
 * valor opaco: el hash que se compara en la base de datos lo calcula el adaptador, y la base de datos decide la vigencia,
 * la espera entre enlaces y el uso unico.
 */
public interface RecuperacionClaveRepositoryPort {

    /**
     * Usuario al que se le puede enviar el enlace: activo, unico dueno del correo y sin un enlace emitido dentro de la
     * espera minima. Para cualquier otro caso devuelve vacio, sin distinguir el motivo.
     */
    Optional<Solicitante> consultarSolicitante(String correo);

    /** Usuario dueno de un enlace que todavia sirve, <b>sin consumirlo</b>. Vacio para cualquier enlace que no sirve. */
    Optional<UUID> consultarUsuarioPorToken(String token);

    /**
     * Marca el enlace como usado (un solo uso).
     *
     * @throws co.edu.uco.asistenciasuco.application.exception.validation.ValidationException si el enlace ya no sirve.
     */
    void consumir(String token);

    /** Datos minimos para armar el aviso: nunca incluye la contrasena ni el token. */
    record Solicitante(UUID idUsuario, String primerNombre) {
    }
}
