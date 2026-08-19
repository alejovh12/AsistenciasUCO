package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;

public record CrearUsuarioResponse(
        boolean exitoso,
        String mensajeUsuario
) {

    public static CrearUsuarioResponse from(final CrearUsuarioResultadoDTO resultado) {
        return new CrearUsuarioResponse(resultado.isExitoso(), resultado.getMensajeUsuario());
    }
}
