package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el caso de uso y el contrato del puerto secundario.
 */
public final class ConsultarSesionRepositoryMapper {

    private ConsultarSesionRepositoryMapper() {
    }

    public static ConsultarSesionRepositoryDTO toRepositoryDTO(final ConsultarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        return new ConsultarSesionRepositoryDTO(domain.getSesion());
    }

    public static SesionConsultadaEntity toUseCaseEntity(final SesionRepositoryProjection entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La entidad de sesion consultada es obligatoria.");
        }

        return new SesionConsultadaEntity(
                entity.getSesion(),
                entity.getGrupo(),
                entity.getTema(),
                entity.getDescripcion(),
                entity.isCerrada(),
                entity.getObservacionCierre()
        );
    }
}
