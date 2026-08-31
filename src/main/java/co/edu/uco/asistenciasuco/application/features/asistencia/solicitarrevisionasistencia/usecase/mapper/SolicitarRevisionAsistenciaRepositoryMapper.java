package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class SolicitarRevisionAsistenciaRepositoryMapper {

    private SolicitarRevisionAsistenciaRepositoryMapper() {
    }

    public static SolicitarRevisionAsistenciaRepositoryDTO toRepositoryDTO(
            final SolicitarRevisionAsistenciaDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }

        return new SolicitarRevisionAsistenciaRepositoryDTO(
                domain.getAsistencia(),
                domain.getMotivo()
        );
    }
}
