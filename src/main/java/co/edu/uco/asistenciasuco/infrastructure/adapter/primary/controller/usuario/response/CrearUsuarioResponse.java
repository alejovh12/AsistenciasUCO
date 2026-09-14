package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;

public record CrearUsuarioResponse(
        boolean exitoso,
        String mensajeUsuario
) {

    public static CrearUsuarioResponse from(final ProvisionarUsuarioResultadoDTO resultado) {
        return new CrearUsuarioResponse(resultado.exitoso(), resultado.mensajeUsuario());
    }
}
