package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto;

import java.util.UUID;

public final class GenerarSesionesGrupoDTO {

    private UUID grupo;
    private UUID usuarioEjecutor;

    public GenerarSesionesGrupoDTO() {
        super();
    }

    public GenerarSesionesGrupoDTO(final UUID grupo, final UUID usuarioEjecutor) {
        setGrupo(grupo);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }
}
