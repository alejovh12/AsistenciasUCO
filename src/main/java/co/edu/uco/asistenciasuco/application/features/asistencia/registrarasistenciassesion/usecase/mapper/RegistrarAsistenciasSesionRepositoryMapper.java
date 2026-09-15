package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class RegistrarAsistenciasSesionRepositoryMapper {

    private RegistrarAsistenciasSesionRepositoryMapper() {
    }

    public static RegistrarAsistenciasSesionRepositoryDTO toRepositoryDTO(final RegistrarAsistenciasSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencias de sesion es obligatorio.");
        }
        return new RegistrarAsistenciasSesionRepositoryDTO(
                domain.getSesion(),
                domain.getRegistros().stream()
                        .map(registro -> new RegistroAsistenciaSesionRepositoryDTO(
                                registro.getEstudiante(),
                                registro.getEstado()
                        ))
                        .toList()
        );
    }
}
