package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record EjecutarCargaUsuariosDomain(
        UUID usuarioEjecutor,
        PerfilDestinoCarga perfilDestino,
        byte[] contenidoBytes,
        String huella
) {

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EjecutarCargaUsuariosDomain that)) {
            return false;
        }
        return Objects.equals(usuarioEjecutor, that.usuarioEjecutor) && Objects.equals(perfilDestino, that.perfilDestino) && Objects.equals(huella, that.huella) && Arrays.equals(contenidoBytes, that.contenidoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioEjecutor, perfilDestino, huella) * 31 + Arrays.hashCode(contenidoBytes);
    }

    @Override
    public String toString() {
        return "EjecutarCargaUsuariosDomain[usuarioEjecutor=" + usuarioEjecutor + ", perfilDestino=" + perfilDestino + ", contenidoBytes=" + (contenidoBytes == null ? 0 : contenidoBytes.length) + " bytes]";
    }
}
