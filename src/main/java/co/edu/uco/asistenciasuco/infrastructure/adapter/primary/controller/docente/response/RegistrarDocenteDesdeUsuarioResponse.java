package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;

public record RegistrarDocenteDesdeUsuarioResponse(
        boolean exitoso,
        String mensajeUsuario
) {

    public static RegistrarDocenteDesdeUsuarioResponse from(final RegistrarDocenteDesdeUsuarioResultadoDTO resultado) {
        return new RegistrarDocenteDesdeUsuarioResponse(resultado.isExitoso(), resultado.getMensajeUsuario());
    }
}
