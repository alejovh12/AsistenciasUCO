package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto;

import java.util.List;
import java.util.UUID;

public final class RegistrarAsistenciasSesionDTO {

    private UUID sesion;
    private List<RegistroAsistenciaSesionDTO> registros;

    public RegistrarAsistenciasSesionDTO() {
        super();
    }

    public RegistrarAsistenciasSesionDTO(final UUID sesion, final List<RegistroAsistenciaSesionDTO> registros) {
        setSesion(sesion);
        setRegistros(registros);
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
}
