package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

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
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
        if (EMPTY_UUID.equals(usuario)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_INVALIDO);
        }
    }

    public UUID getUsuario() {
        return usuario;
    }

}
