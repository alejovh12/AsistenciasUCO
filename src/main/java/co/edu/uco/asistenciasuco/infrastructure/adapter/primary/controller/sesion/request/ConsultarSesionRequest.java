package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

public final class ConsultarSesionRequest {

    @NonEmptyUuid(message = "El identificador de la sesion no es valido.")
    @NotNull(message = "La sesion es obligatoria.")
    private UUID sesion;

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }
}
