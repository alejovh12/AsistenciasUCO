package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaRepositoryDTO {

    private UUID asistencia;
    private String motivo;
    private UUID idCorrelacion;

    public SolicitarRevisionAsistenciaRepositoryDTO() {
        super();
    }

    public SolicitarRevisionAsistenciaRepositoryDTO(final UUID asistencia, final String motivo, final UUID idCorrelacion) {
        setAsistencia(asistencia);
        setMotivo(motivo);
        setIdCorrelacion(idCorrelacion);
    }

    public UUID getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(final UUID asistencia) {
        this.asistencia = asistencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public void setIdCorrelacion(final UUID idCorrelacion) {
        this.idCorrelacion = idCorrelacion;
    }
}
