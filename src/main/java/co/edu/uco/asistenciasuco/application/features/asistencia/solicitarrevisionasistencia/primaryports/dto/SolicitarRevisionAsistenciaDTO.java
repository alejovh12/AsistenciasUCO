package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para solicitar la revision de una asistencia.
 */
public final class SolicitarRevisionAsistenciaDTO {

    private UUID asistencia;
    private String motivo;
    private UUID idCorrelacion;

    public SolicitarRevisionAsistenciaDTO() {
        super();
    }

    public SolicitarRevisionAsistenciaDTO(final UUID asistencia, final String motivo, final UUID idCorrelacion) {
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
