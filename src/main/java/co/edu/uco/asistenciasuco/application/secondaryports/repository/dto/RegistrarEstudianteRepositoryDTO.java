package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para registrar un estudiante en un grupo.
 */
public final class RegistrarEstudianteRepositoryDTO {

    private UUID estudiante;
    private UUID grupo;
    private UUID idCorrelacion;

    public RegistrarEstudianteRepositoryDTO() {
        super();
    }

    public RegistrarEstudianteRepositoryDTO(final UUID estudiante, final UUID grupo, final UUID idCorrelacion) {
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
