package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record ValidarCargaUsuariosDomain(
        UUID usuarioEjecutor,
        PerfilDestinoCarga perfilDestino,
        byte[] contenidoBytes
) {

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidarCargaUsuariosDomain that)) {
            return false;
        }
        return Objects.equals(usuarioEjecutor, that.usuarioEjecutor) && Objects.equals(perfilDestino, that.perfilDestino) && Arrays.equals(contenidoBytes, that.contenidoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioEjecutor, perfilDestino) * 31 + Arrays.hashCode(contenidoBytes);
    }

    @Override
    public String toString() {
        return "ValidarCargaUsuariosDomain[usuarioEjecutor=" + usuarioEjecutor + ", perfilDestino=" + perfilDestino + ", contenidoBytes=" + (contenidoBytes == null ? 0 : contenidoBytes.length) + " bytes]";
    }
}
