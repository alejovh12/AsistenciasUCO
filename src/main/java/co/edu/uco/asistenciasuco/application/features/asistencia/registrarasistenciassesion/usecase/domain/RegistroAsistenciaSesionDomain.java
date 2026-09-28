package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.Set;
import java.util.UUID;

public final class RegistroAsistenciaSesionDomain {

    /**
     * Contrato publico definitivo de estados de asistencia. La DB puede conservar codigos
     * historicos (A, F, J, T, etc.) en {@code dbo.RazonCausa}, pero el backend nunca los acepta
     * ni los expone: solo AN (asistencia normal), SJC (sin justa causa) y EX (excusa).
     */
    public static final Set<String> ESTADOS_VALIDOS = Set.of("AN", "SJC", "EX");

    private final UUID estudiante;
    private final String estado;

    public RegistroAsistenciaSesionDomain(final UUID estudiante, final String estado) {
        if (ObjectHelper.isNull(estudiante)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION);
        }
        final String estadoNormalizado = TextHelper.normalizeTrimUpper(estado);
        if (TextHelper.isNullOrBlank(estadoNormalizado)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_REQUERIDO);
        }
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO);
        }
        this.estudiante = estudiante;
        this.estado = estadoNormalizado;
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public String getEstado() {
        return estado;
    }
}
