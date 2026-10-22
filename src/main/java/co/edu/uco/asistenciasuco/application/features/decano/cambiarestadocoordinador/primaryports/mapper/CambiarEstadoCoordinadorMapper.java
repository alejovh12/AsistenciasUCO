package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.domain.CambiarEstadoCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre los DTO del puerto primario y el dominio de cambiar el estado de un coordinador.
 */
public final class CambiarEstadoCoordinadorMapper {

    private CambiarEstadoCoordinadorMapper() {
    }

    public static CambiarEstadoCoordinadorDomain toDomain(final CambiarEstadoCoordinadorDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para cambiar el estado del coordinador es obligatorio.");
        }
        return new CambiarEstadoCoordinadorDomain(dto.idCoordinador(), dto.estaActivo(), dto.usuario());
    }

    public static ResultadoGestionCoordinadorDTO toDTO(final ResultadoGestionCoordinadorDomain domain) {
        return new ResultadoGestionCoordinadorDTO(
                domain.idCoordinador(),
                domain.sincronizacionPendiente()
                        ? ResultadoGestionCoordinadorDTO.SINCRONIZACION_PENDIENTE
                        : ResultadoGestionCoordinadorDTO.SINCRONIZACION_OK,
                domain.mensajeUsuario());
    }
}
