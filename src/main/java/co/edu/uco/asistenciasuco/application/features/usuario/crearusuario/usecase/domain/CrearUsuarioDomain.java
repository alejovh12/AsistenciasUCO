package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.usuario.domain.UsuarioRegistroDomain;

import java.util.UUID;

/**
 * Dominio de la operacion crear usuario base.
 */
public final class CrearUsuarioDomain {

    private final UsuarioRegistroDomain usuarioRegistro;

    private CrearUsuarioDomain(final UsuarioRegistroDomain usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public static CrearUsuarioDomain crear(
            final UUID tipoIdIdentificacion,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        return new CrearUsuarioDomain(UsuarioRegistroDomain.crear(
                tipoIdIdentificacion,
                numeroIdentificacion,
                primerApellido,
                segundoApellido,
                primerNombre,
                segundoNombre,
                correo,
                password
        ));
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

}
