package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain.RegistrarAsistenciaAutonomaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

public final class RegistrarAsistenciaAutonomaRepositoryMapper {

    private RegistrarAsistenciaAutonomaRepositoryMapper() {
    }

    public static RegistrarAsistenciaAutonomaRepositoryDTO toRepositoryDTO(
            final RegistrarAsistenciaAutonomaDomain domain,
            final UUID estudianteId
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencia autonoma es obligatorio.");
        }
        return new RegistrarAsistenciaAutonomaRepositoryDTO(
                estudianteId,
                domain.getSesion(),
                domain.getCodigoVerificacion()
        );
    }
}
