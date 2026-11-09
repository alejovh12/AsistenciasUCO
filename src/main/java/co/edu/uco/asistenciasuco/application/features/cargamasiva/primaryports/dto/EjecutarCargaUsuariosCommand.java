package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record EjecutarCargaUsuariosCommand(
        UUID usuarioEjecutor,
        PerfilDestinoCarga perfilDestino,
        String nombreArchivo,
        byte[] contenidoBytes,
        String huella
) {
    public EjecutarCargaUsuariosCommand {
        Objects.requireNonNull(usuarioEjecutor, "El usuario ejecutor es obligatorio.");
        Objects.requireNonNull(perfilDestino, "El perfil destino es obligatorio.");
        Objects.requireNonNull(huella, "La huella es obligatoria.");
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
        if (!(other instanceof EjecutarCargaUsuariosCommand that)) {
            return false;
        }
        return Objects.equals(usuarioEjecutor, that.usuarioEjecutor) && Objects.equals(perfilDestino, that.perfilDestino) && Objects.equals(nombreArchivo, that.nombreArchivo) && Objects.equals(huella, that.huella) && Arrays.equals(contenidoBytes, that.contenidoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioEjecutor, perfilDestino, nombreArchivo, huella) * 31 + Arrays.hashCode(contenidoBytes);
    }

    @Override
    public String toString() {
        return "EjecutarCargaUsuariosCommand[usuarioEjecutor=" + usuarioEjecutor + ", perfilDestino=" + perfilDestino + ", nombreArchivo=" + nombreArchivo + ", contenidoBytes=" + (contenidoBytes == null ? 0 : contenidoBytes.length) + " bytes]";
    }
}
