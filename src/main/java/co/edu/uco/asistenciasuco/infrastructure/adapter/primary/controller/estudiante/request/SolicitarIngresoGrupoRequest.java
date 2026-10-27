package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request;

import java.util.UUID;

/** HU053: cuerpo de {@code POST /api/v1/estudiante/solicitudes-matricula}. */
public final class SolicitarIngresoGrupoRequest {

    private UUID grupoId;

    public UUID getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(final UUID grupoId) {
        this.grupoId = grupoId;
    }
}
