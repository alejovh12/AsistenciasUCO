package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Dominio de la operacion registrar docente desde usuario existente.
 */
public final class RegistrarDocenteDesdeUsuarioDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UUID usuario;

    private RegistrarDocenteDesdeUsuarioDomain(final UUID usuario) {
        this.usuario = usuario;
    }

    public static RegistrarDocenteDesdeUsuarioDomain crear(final UUID usuario) {
        validarUsuario(usuario);
        return new RegistrarDocenteDesdeUsuarioDomain(usuario);
    }

    private static void validarUsuario(final UUID usuario) {
        if (Objects.isNull(usuario)) {
            throw new ValidationException("ERR_USUARIO_REQUERIDO", "El usuario es obligatorio.");
        }
        if (EMPTY_UUID.equals(usuario)) {
            throw new ValidationException("ERR_USUARIO_INVALIDO", "El usuario debe ser valido.");
        }
    }

    public UUID getUsuario() {
        return usuario;
    }

}
