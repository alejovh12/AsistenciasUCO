package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class CerrarSesionMapper {

    private CerrarSesionMapper() {
    }

    public static CerrarSesionDomain toDomain(final CerrarSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para cerrar sesion es obligatorio.");
        }

        return new CerrarSesionDomain(
                dto.getSesion(),
                dto.getObservacionCierre()
        );
    }
}
