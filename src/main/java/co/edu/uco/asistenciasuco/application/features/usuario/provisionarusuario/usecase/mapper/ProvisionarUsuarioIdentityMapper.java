package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.UUID;

/**
 * Mapper entre el dominio de provisionar usuario y los contratos de los puertos secundarios
 * (creacion de usuario base y proveedor de identidad).
 */
public final class ProvisionarUsuarioIdentityMapper {

    private ProvisionarUsuarioIdentityMapper() {
    }

    public static CrearUsuarioDomain toCrearUsuarioDomain(final ProvisionarUsuarioDomain domain) {
        return CrearUsuarioDomain.of(domain.getUsuarioRegistro());
    }

    public static CrearCuentaIdentidadDTO toCrearCuentaIdentidadDTO(
            final ProvisionarUsuarioDomain domain,
            final UUID usuarioId,
            final InstitutionalRole rolInstitucional
    ) {
        return new CrearCuentaIdentidadDTO(
                String.valueOf(domain.getNumeroIdentificacion()),
                usuarioId,
                domain.getCorreo(),
                domain.getPrimerNombre(),
                domain.getPrimerApellido(),
                domain.resolverCredencialNueva(),
                rolInstitucional
        );
    }

    /**
     * El resultado de Identity ({@code CuentaIdentidadDTO}) es neutral y no transporta mensaje
     * humano alguno del IdP — el mensaje funcional devuelto a Application/API es exclusivamente
     * el de la creación del usuario en la base de datos institucional, por lo que este mapper
     * no necesita ni consume el resultado de Identity para construirlo.
     */
    public static ProvisionarUsuarioResultadoEntity toResultadoEntity(final CrearUsuarioResultadoEntity usuario) {
        return new ProvisionarUsuarioResultadoEntity(
                usuario.getUsuarioId(),
                usuario.isExitoso(),
                usuario.getMensajeUsuario()
        );
    }
}
