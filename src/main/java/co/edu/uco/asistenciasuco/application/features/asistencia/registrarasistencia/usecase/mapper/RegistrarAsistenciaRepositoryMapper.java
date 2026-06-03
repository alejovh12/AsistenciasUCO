package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class RegistrarAsistenciaRepositoryMapper {

    private RegistrarAsistenciaRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarAsistenciaRepositoryDTO toRepositoryDTO(final RegistrarAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }

        return new RegistrarAsistenciaRepositoryDTO(
                domain.getEstudiante(),
                domain.getGrupo(),
                domain.getSesion(),
                domain.isPresente(),
                domain.getObservacion(),
                domain.getIdCorrelacion()
        );
    }
}
