package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

public final class ConsultarDocentePorIdRequest {

    @NonEmptyUuid(message = "El identificador del docente no es valido.")
    @NotNull(message = "El docente es obligatorio.")
    private UUID docente;

    public UUID getDocente() {
        return docente;
    }

    public void setDocente(final UUID docente) {
        this.docente = docente;
    }
}
