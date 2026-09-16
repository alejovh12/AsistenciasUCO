package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto;

import java.util.List;
import java.util.UUID;

public final class RegistrarAsistenciasSesionDTO {

    private UUID sesion;
    private List<RegistroAsistenciaSesionDTO> registros;
    private UUID usuarioEjecutor;

    public RegistrarAsistenciasSesionDTO() {
        super();
    }

    public RegistrarAsistenciasSesionDTO(
            final UUID sesion,
            final List<RegistroAsistenciaSesionDTO> registros,
            final UUID usuarioEjecutor
    ) {
        setSesion(sesion);
        setRegistros(registros);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public List<RegistroAsistenciaSesionDTO> getRegistros() {
        return registros;
    }

    public void setRegistros(final List<RegistroAsistenciaSesionDTO> registros) {
        this.registros = registros;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }
}
