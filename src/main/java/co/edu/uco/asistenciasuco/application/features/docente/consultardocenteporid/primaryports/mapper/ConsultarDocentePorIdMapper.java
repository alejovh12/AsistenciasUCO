package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir el DTO de consulta por ID al dominio.
 */
public final class ConsultarDocentePorIdMapper {

    private ConsultarDocentePorIdMapper() {
    }

    public static ConsultarDocentePorIdDomain toDomain(final ConsultarDocentePorIdDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar docente por ID es obligatorio.");
        }

        return ConsultarDocentePorIdDomain.crear(dto.getDocente());
    }
}
