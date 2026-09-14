package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request;

import java.util.UUID;

public final class RegistrarAsistenciaQrRequest {

    private UUID sesionId;
    private String codigo;
    private String codigoAcceso;

    public UUID getSesionId() {
        return sesionId;
    }

    public void setSesionId(final UUID sesionId) {
        this.sesionId = sesionId;
    }

    public String getCodigo() {
        return codigo == null ? codigoAcceso : codigo;
    }

    public void setCodigo(final String codigo) {
        this.codigo = codigo;
    }

    public void setCodigoAcceso(final String codigoAcceso) {
        this.codigoAcceso = codigoAcceso;
    }
}
