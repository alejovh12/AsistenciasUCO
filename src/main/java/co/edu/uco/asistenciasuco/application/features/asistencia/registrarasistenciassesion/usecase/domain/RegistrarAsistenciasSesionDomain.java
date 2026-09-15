package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.UUID;

public final class RegistrarAsistenciasSesionDomain {

    private final UUID sesion;
    private final List<RegistroAsistenciaSesionDomain> registros;

    public RegistrarAsistenciasSesionDomain(final UUID sesion, final List<RegistroAsistenciaSesionDomain> registros) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SESION_ASISTENCIA_REQUERIDA);
        }
        if (ObjectHelper.isNull(registros) || registros.isEmpty()) {
            throw new ValidationException(AsistenciaErrorCode.ERR_REGISTROS_ASISTENCIA_REQUERIDOS);
        }
        this.sesion = sesion;
        this.registros = List.copyOf(registros);
    }

    public UUID getSesion() {
        return sesion;
    }

    public List<RegistroAsistenciaSesionDomain> getRegistros() {
        return registros;
    }
}
