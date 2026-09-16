package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar las sesiones de un grupo.
 */
public final class ConsultarSesionesPorGrupoDTO {

    private UUID grupo;
    private UUID usuarioEjecutor;

    public ConsultarSesionesPorGrupoDTO() {
        super();
    }

    public ConsultarSesionesPorGrupoDTO(final UUID grupo, final UUID usuarioEjecutor) {
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
