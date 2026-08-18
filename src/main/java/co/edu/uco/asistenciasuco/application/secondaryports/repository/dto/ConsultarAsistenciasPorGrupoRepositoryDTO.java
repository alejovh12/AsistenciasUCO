package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoRepositoryDTO {

    private UUID grupo;
    private UUID sesion;

    public ConsultarAsistenciasPorGrupoRepositoryDTO() {
        super();
    }

    public ConsultarAsistenciasPorGrupoRepositoryDTO(final UUID grupo, final UUID sesion) {
        setGrupo(grupo);
        setSesion(sesion);
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

}
