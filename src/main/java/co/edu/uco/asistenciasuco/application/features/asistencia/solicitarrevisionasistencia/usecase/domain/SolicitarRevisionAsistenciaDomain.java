package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaDomain {

    private final UUID asistencia;
    private final String motivo;
    private final UUID idCorrelacion;

    public SolicitarRevisionAsistenciaDomain(
            final UUID asistencia,
            final String motivo,
            final UUID idCorrelacion
    ) {
        validarAsistencia(asistencia);
        this.motivo = validarMotivo(motivo);
        validarIdCorrelacion(idCorrelacion);

        this.asistencia = asistencia;
        this.idCorrelacion = idCorrelacion;
    }

    private void validarAsistencia(final UUID asistencia) {
        if (ObjectHelper.isNull(asistencia)) {
            throw new IllegalArgumentException("La asistencia es obligatoria.");
        }
    }

    private String validarMotivo(final String motivo) {
        final String motivoNormalizado = TextHelper.trim(motivo);

        if (TextHelper.isNullOrBlank(motivoNormalizado)) {
            throw new IllegalArgumentException("El motivo de revision es obligatorio.");
        }

        if (!TextHelper.hasLengthBetween(motivoNormalizado, 10, 300)) {
            throw new IllegalArgumentException("El motivo de revision debe tener entre 10 y 300 caracteres.");
        }

        return motivoNormalizado;
    }

    private void validarIdCorrelacion(final UUID idCorrelacion) {
        if (ObjectHelper.isNull(idCorrelacion)) {
            throw new IllegalArgumentException("El id de correlacion es obligatorio.");
        }
    }

    public UUID getAsistencia() {
        return asistencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
