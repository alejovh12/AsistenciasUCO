package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para registrar un estudiante en un grupo.
 */
public final class RegistrarEstudianteDTO {

    private UUID estudiante;
    private UUID grupo;
    private UUID idCorrelacion;

    public RegistrarEstudianteDTO() {
        super();
    }

    public RegistrarEstudianteDTO(final UUID estudiante, final UUID grupo, final UUID idCorrelacion) {
        setEstudiante(estudiante);
        setGrupo(grupo);
        setIdCorrelacion(idCorrelacion);
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(final UUID estudiante) {
        this.estudiante = estudiante;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
