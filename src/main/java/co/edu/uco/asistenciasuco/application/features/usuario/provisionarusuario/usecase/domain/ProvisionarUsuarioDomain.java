package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.usuario.domain.UsuarioRegistroDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;

import java.util.UUID;

/**
 * Dominio de la operacion provisionar usuario.
 */
public final class ProvisionarUsuarioDomain {

    private final UsuarioRegistroDomain usuarioRegistro;

    private ProvisionarUsuarioDomain(final UsuarioRegistroDomain usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public static ProvisionarUsuarioDomain crear(
            final UUID tipoIdIdentificacion,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        return new ProvisionarUsuarioDomain(UsuarioRegistroDomain.crear(
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

    public UsuarioRegistroDomain getUsuarioRegistro() {
        return usuarioRegistro;
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

    public String resolverCredencialNueva() {
        return PasswordRegistroRule.resolverCredencialNueva(getPassword(), getNumeroIdentificacion());
    }
}
