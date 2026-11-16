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

    /**
     * Solicitud de cuenta del decano. IDENT-002: sin contrasena; una cuenta nueva queda pendiente hasta que su titular la
     * activa con el enlace del correo (una persona que ya tenia cuenta no cambia).
     */
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
                null,
                InstitutionalRole.DECANO,
                true
        );
    }
}
