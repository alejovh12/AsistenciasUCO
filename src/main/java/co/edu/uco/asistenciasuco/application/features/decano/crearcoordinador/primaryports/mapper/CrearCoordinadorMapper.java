package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el DTO de entrada y el dominio de crear coordinador.
 */
public final class CrearCoordinadorMapper {

    private CrearCoordinadorMapper() {
    }

    public static CrearCoordinadorDomain toDomain(final CrearCoordinadorDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear coordinador es obligatorio.");
        }

        return new CrearCoordinadorDomain(
                dto.numeroIdentificacion(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.correo(),
                dto.idPrograma(),
                dto.password(),
                dto.usuario()
        );
    }
}
