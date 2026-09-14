package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto;

import java.util.UUID;

public final class RegistroAsistenciaSesionDTO {

    private UUID estudiante;
    private String estado;

    public RegistroAsistenciaSesionDTO() {
        super();
    }

    public RegistroAsistenciaSesionDTO(final UUID estudiante, final String estado) {
        setEstudiante(estudiante);
        setEstado(estado);
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(final UUID estudiante) {
        this.estudiante = estudiante;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(final String estado) {
        this.estado = estado;
    }
}
