package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorResultado;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.ProgramaNuevoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

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
                dto.tipoIdentificacionId(),
                dto.numeroIdentificacion(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.correo(),
                dto.idPrograma(),
                dto.password(),
                dto.usuario(),
                dto.programaNuevo() == null ? null : new ProgramaNuevoDomain(dto.programaNuevo().nombre(),
                        dto.programaNuevo().idTipoPrograma(), dto.programaNuevo().idModalidad(), dto.programaNuevo().idJornada())
        );
    }

    public static CrearCoordinadorResultadoDTO toResultadoDto(final CrearCoordinadorResultado resultado) {
        return new CrearCoordinadorResultadoDTO(resultado.idCoordinador(), resultado.idPrograma(), resultado.nombrePrograma());
    }
}
