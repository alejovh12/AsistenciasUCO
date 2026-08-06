package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.entity.TipoIdentificacionConsultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir entre modelos internos del caso de uso y DTOs.
 */
public final class ConsultarTiposIdentificacionMapper {

    private ConsultarTiposIdentificacionMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static List<TipoIdentificacionDTO> toDTOs(final List<TipoIdentificacionConsultadoEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<TipoIdentificacionDTO> resultado = new ArrayList<>();

        for (final TipoIdentificacionConsultadoEntity entity : entities) {
            resultado.add(new TipoIdentificacionDTO(
                    entity.getId(),
                    entity.getTipoIdentificacion(),
                    entity.getNombre()
            ));
        }

        return resultado;
    }
}
