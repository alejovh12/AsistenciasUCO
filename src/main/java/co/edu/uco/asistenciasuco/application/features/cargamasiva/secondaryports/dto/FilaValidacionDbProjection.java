package co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto;

import java.util.UUID;

public record FilaValidacionDbProjection(
        int numeroFila,
        boolean esValida,
        boolean yaExiste,
        String codigoMensaje,
        String mensajeUsuario,
        UUID idTipoIdentificacion,
        UUID idTipoVinculacion,
        UUID idPrograma,
        UUID idFacultad,
        UUID idUsuarioExistente
) {
}
