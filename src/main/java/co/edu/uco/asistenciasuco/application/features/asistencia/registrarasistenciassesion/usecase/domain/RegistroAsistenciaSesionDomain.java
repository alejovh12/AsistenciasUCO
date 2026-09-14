package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

public final class RegistroAsistenciaSesionDomain {

    private final UUID estudiante;
    private final String estado;

    public RegistroAsistenciaSesionDomain(final UUID estudiante, final String estado) {
        if (ObjectHelper.isNull(estudiante)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION);
        }
        final String estadoNormalizado = TextHelper.trim(estado);
        if (TextHelper.isNullOrBlank(estadoNormalizado)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_REQUERIDO);
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
