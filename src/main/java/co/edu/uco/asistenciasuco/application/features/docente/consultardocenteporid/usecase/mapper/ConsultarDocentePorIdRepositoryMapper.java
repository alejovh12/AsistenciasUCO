package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio de consulta por ID y el puerto secundario.
 */
public final class ConsultarDocentePorIdRepositoryMapper {

    private ConsultarDocentePorIdRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static ConsultarDocentePorIdRepositoryDTO toRepositoryDTO(final ConsultarDocentePorIdDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar docente por ID es obligatorio.");
        }

        return new ConsultarDocentePorIdRepositoryDTO(domain.getDocente());
    }
}
