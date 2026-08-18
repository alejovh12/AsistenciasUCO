package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class CrearSesionMapper {

    private CrearSesionMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static CrearSesionDomain toDomain(final CrearSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear sesion es obligatorio.");
        }

        return new CrearSesionDomain(
                dto.getGrupo(),
                dto.getTema(),
                dto.getDescripcion()
        );
    }
}
