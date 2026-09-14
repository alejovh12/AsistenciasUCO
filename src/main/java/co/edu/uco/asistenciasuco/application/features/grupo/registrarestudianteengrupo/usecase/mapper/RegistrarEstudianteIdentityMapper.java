package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;

import java.util.Objects;

/**
 * Mapper entre el dominio de registrar estudiante en grupo y el contrato del proveedor de
 * identidad. Es propio de esta feature — no reutiliza el mapper de {@code provisionarusuario}
 * para no acoplar dominios de features distintas.
 */
public final class RegistrarEstudianteIdentityMapper {

    private RegistrarEstudianteIdentityMapper() {
    }

    /**
     * @param usuarioCanonico id y correo leidos DESPUES del command de registro.
     * @param rolInstitucional rol determinado por el servidor para este flujo — siempre ESTUDIANTE.
     */
    public static CrearCuentaIdentidadDTO toCrearCuentaIdentidadDTO(
            final RegistrarEstudianteDomain domain,
            final UsuarioIdentidadRepositoryProjection usuarioCanonico,
            final InstitutionalRole rolInstitucional
    ) {
        return new CrearCuentaIdentidadDTO(
                String.valueOf(domain.getNumeroIdentificacion()),
                Objects.requireNonNull(usuarioCanonico, "La identidad canonica post-command es obligatoria.").id(),
                usuarioCanonico.correo(),
                domain.getPrimerNombre(),
                domain.getPrimerApellido(),
                domain.getPassword(),
                rolInstitucional
        );
    }
}
