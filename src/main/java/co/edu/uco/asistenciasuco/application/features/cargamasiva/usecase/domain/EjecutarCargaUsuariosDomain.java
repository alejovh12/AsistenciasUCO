package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.UUID;

public record EjecutarCargaUsuariosDomain(
        UUID usuarioEjecutor,
        PerfilDestinoCarga perfilDestino,
        byte[] contenidoBytes,
        String huella
) {
}
