package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

public final class RegistrarAsistenciasSesionRequest {

    private UUID sesionId;
    @Valid
    private List<RegistroAsistenciaRequest> registros;

    public UUID getSesionId() {
        return sesionId;
    }

    public void setSesionId(final UUID sesionId) {
        this.sesionId = sesionId;
    }

    public List<RegistroAsistenciaRequest> getRegistros() {
        return registros;
    }

    public void setRegistros(final List<RegistroAsistenciaRequest> registros) {
        this.registros = registros;
    }
}
