package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

public final class ConsultarAsistenciasPorGrupoRequest {

    @NonEmptyUuid(message = "El identificador del grupo no es valido.")
    @NotNull(message = "El grupo es obligatorio.")
    private UUID grupo;
    @NonEmptyUuid(message = "El identificador de la sesion no es valido.")
    private UUID sesion;

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }
}
