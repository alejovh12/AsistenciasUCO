package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.UsuarioRegistroDomain;

import java.util.UUID;

/**
 * Dominio de la operacion registrar estudiante en grupo.
 */
public final class RegistrarEstudianteDomain {

    private final UsuarioRegistroDomain usuarioRegistro;
    private final UUID grupoId;

    public RegistrarEstudianteDomain(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password,
            final UUID grupoId
    ) {
        this.usuarioRegistro = UsuarioRegistroDomain.crear(
                tipoIdentificacionId,
                numeroIdentificacion,
                primerApellido,
                segundoApellido,
                primerNombre,
                segundoNombre,
                correo,
                password
        );
        this.grupoId = validarGrupo(grupoId);
    }

    private UUID validarGrupo(final UUID grupoId) {
        if (grupoId == null) {
            throw new ValidationException("ERR_GRUPO_REQUERIDO", "El grupo es obligatorio.");
        }
        if (new UUID(0L, 0L).equals(grupoId)) {
            throw new ValidationException("ERR_GRUPO_INVALIDO", "Debe seleccionar un grupo valido.");
        }
        return grupoId;
    }

    public UUID getTipoIdentificacionId() {
        return usuarioRegistro.getTipoIdentificacionId();
    }

    public Integer getNumeroIdentificacion() {
        return usuarioRegistro.getNumeroIdentificacion();
    }

    public String getPrimerApellido() {
        return usuarioRegistro.getPrimerApellido();
    }

    public String getSegundoApellido() {
        return usuarioRegistro.getSegundoApellido();
    }

    public String getPrimerNombre() {
        return usuarioRegistro.getPrimerNombre();
    }

    public String getSegundoNombre() {
        return usuarioRegistro.getSegundoNombre();
    }

    public String getCorreo() {
        return usuarioRegistro.getCorreo();
    }

    public String getPassword() {
        return usuarioRegistro.getPassword();
    }

    public UUID getGrupoId() {
        return grupoId;
    }
}
