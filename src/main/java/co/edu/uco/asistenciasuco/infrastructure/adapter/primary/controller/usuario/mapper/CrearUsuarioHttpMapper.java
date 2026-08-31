package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;

import java.util.Objects;

public final class CrearUsuarioHttpMapper {

    private CrearUsuarioHttpMapper() {
    }

    public static CrearUsuarioDTO toApplicationDTO(final CrearUsuarioRequest request) {
        Objects.requireNonNull(request, "El request HTTP para crear usuario es obligatorio.");
        return new CrearUsuarioDTO(
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
