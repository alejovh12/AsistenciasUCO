package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto;

import java.util.UUID;

public final class GenerarSesionesGrupoDTO {

    private UUID grupo;

    public GenerarSesionesGrupoDTO() {
        super();
    }

    public GenerarSesionesGrupoDTO(final UUID grupo) {
        setGrupo(grupo);
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }
}
