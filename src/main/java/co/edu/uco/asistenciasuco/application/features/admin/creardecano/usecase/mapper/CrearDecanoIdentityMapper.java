package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.Objects;

/** Construye la identidad del decano desde la proyeccion institucional post-command. */
public final class CrearDecanoIdentityMapper {

    private CrearDecanoIdentityMapper() {
    }

    /** Solicitud de cuenta del decano; el administrador responde por el correo, asi que no queda pendiente de verificacion. */
    public static SolicitudCuentaIdentidad toSolicitudCuentaIdentidad(
            final CrearDecanoDomain domain,
            final UsuarioIdentidadRepositoryProjection usuarioCanonico
    ) {
        Objects.requireNonNull(domain, "El dominio del decano es obligatorio.");
        Objects.requireNonNull(usuarioCanonico, "El usuario canonico es obligatorio.");
        return new SolicitudCuentaIdentidad(
                usuarioCanonico.id(),
                String.valueOf(usuarioCanonico.numeroIdentificacion()),
                usuarioCanonico.correo(),
                usuarioCanonico.primerNombre(),
                usuarioCanonico.primerApellido(),
                domain.getPassword(),
                InstitutionalRole.DECANO,
                false
        );
    }
}
