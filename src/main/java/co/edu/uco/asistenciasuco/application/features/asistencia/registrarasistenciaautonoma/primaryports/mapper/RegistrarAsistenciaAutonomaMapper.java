package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain.RegistrarAsistenciaAutonomaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

public final class RegistrarAsistenciaAutonomaMapper {

    private RegistrarAsistenciaAutonomaMapper() {
    }

    public static RegistrarAsistenciaAutonomaDomain toDomain(final RegistrarAsistenciaAutonomaDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar asistencia autonoma es obligatorio.");
        }
        return new RegistrarAsistenciaAutonomaDomain(
                dto.getSesion(),
                dto.getCodigoVerificacion(),
                dto.getUsuario()
        );
    }
}
