package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request;

import java.util.UUID;

public final class RegistroAsistenciaRequest {

    private UUID estudianteId;
    private String estado;

    public UUID getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(final UUID estudianteId) {
        this.estudianteId = estudianteId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(final String estado) {
        this.estado = estado;
    }
}
