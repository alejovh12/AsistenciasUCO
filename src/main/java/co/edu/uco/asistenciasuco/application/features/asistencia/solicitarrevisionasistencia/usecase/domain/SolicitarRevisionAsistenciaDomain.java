package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaDomain {

    private final UUID asistencia;
    private final String motivo;

    public SolicitarRevisionAsistenciaDomain(
            final UUID asistencia,
            final String motivo
    ) {
        validarAsistencia(asistencia);
        this.motivo = validarMotivo(motivo);

        this.asistencia = asistencia;
    }

    private void validarAsistencia(final UUID asistencia) {
        if (ObjectHelper.isNull(asistencia)) {
            throw new ValidationException(ErrorCode.ERR_ASISTENCIA_REQUERIDA);
        }
    }

    private String validarMotivo(final String motivo) {
        final String motivoNormalizado = TextHelper.trim(motivo);

        if (TextHelper.isNullOrBlank(motivoNormalizado)) {
            throw new ValidationException(ErrorCode.ERR_MOTIVO_REVISION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(motivoNormalizado, 10, 300)) {
            throw new ValidationException(ErrorCode.ERR_MOTIVO_REVISION_LONGITUD_INVALIDA);
        }

        return motivoNormalizado;
    }

    public UUID getAsistencia() {
        return asistencia;
    }

    public String getMotivo() {
        return motivo;
    }

}
