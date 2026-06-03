package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoDTO {

    private UUID grupo;
    private UUID sesion;
    private UUID idCorrelacion;

    public ConsultarAsistenciasPorGrupoDTO() {
        super();
    }

    public ConsultarAsistenciasPorGrupoDTO(final UUID grupo, final UUID sesion, final UUID idCorrelacion) {
        setGrupo(grupo);
        setSesion(sesion);
        setIdCorrelacion(idCorrelacion);
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
