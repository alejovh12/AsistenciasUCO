package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record ValidarCargaUsuariosCommand(
        UUID usuarioEjecutor,
        PerfilDestinoCarga perfilDestino,
        String nombreArchivo,
        byte[] contenidoBytes
) {
    public ValidarCargaUsuariosCommand {
        Objects.requireNonNull(usuarioEjecutor, "El usuario ejecutor es obligatorio.");
        Objects.requireNonNull(perfilDestino, "El perfil destino es obligatorio.");
        contenidoBytes = contenidoBytes == null ? new byte[0] : Arrays.copyOf(contenidoBytes, contenidoBytes.length);
    }

    @Override
    public byte[] contenidoBytes() {
        return Arrays.copyOf(contenidoBytes, contenidoBytes.length);
    }
}
