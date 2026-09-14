package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;

import java.util.Objects;

public final class CrearUsuarioHttpMapper {

    private CrearUsuarioHttpMapper() {
    }

    public static ProvisionarUsuarioDTO toApplicationDTO(final CrearUsuarioRequest request) {
        Objects.requireNonNull(request, "El request HTTP para crear usuario es obligatorio.");
        return new ProvisionarUsuarioDTO(
                request.getTipoIdIdentificacion(),
                request.getNumeroIdentificacion(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getCorreo(),
                request.getPassword()
        );
    }
}
