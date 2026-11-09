package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;

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

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidarCargaUsuariosCommand that)) {
            return false;
        }
        return Objects.equals(usuarioEjecutor, that.usuarioEjecutor) && Objects.equals(perfilDestino, that.perfilDestino) && Objects.equals(nombreArchivo, that.nombreArchivo) && Arrays.equals(contenidoBytes, that.contenidoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioEjecutor, perfilDestino, nombreArchivo) * 31 + Arrays.hashCode(contenidoBytes);
    }

    @Override
    public String toString() {
        return "ValidarCargaUsuariosCommand[usuarioEjecutor=" + usuarioEjecutor + ", perfilDestino=" + perfilDestino + ", nombreArchivo=" + nombreArchivo + ", contenidoBytes=" + (contenidoBytes == null ? 0 : contenidoBytes.length) + " bytes]";
    }
}
