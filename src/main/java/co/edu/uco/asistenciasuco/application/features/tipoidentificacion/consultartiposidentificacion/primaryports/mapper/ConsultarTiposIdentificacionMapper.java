package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir entre modelos internos del caso de uso y DTOs.
 */
public final class ConsultarTiposIdentificacionMapper {

    private ConsultarTiposIdentificacionMapper() {
    }

    public static List<TipoIdentificacionDTO> toDTOs(final List<TipoIdentificacionDomain> domains) {
        if (ObjectHelper.isNull(domains)) {
            return List.of();
        }

        final List<TipoIdentificacionDTO> resultado = new ArrayList<>();

        for (final TipoIdentificacionDomain domain : domains) {
            resultado.add(new TipoIdentificacionDTO(
                    domain.getId(),
                    domain.getTipoIdentificacion(),
                    domain.getNombre()
            ));
        }

        return resultado;
    }
}
