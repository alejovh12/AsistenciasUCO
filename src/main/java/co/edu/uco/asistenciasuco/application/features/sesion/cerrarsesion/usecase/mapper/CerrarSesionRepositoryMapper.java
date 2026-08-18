package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class CerrarSesionRepositoryMapper {

    private CerrarSesionRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static CerrarSesionRepositoryDTO toRepositoryDTO(final CerrarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }

        return new CerrarSesionRepositoryDTO(
                domain.getSesion(),
                domain.getObservacionCierre()
        );
    }
}
