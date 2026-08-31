package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre el caso de uso y el contrato del puerto secundario.
 */
public final class ConsultarTiposIdentificacionRepositoryMapper {

    private ConsultarTiposIdentificacionRepositoryMapper() {
    }

    public static List<TipoIdentificacionDomain> toDomains(
            final List<TipoIdentificacionRepositoryProjection> entities
    ) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<TipoIdentificacionDomain> resultado = new ArrayList<>();

        for (final TipoIdentificacionRepositoryProjection entity : entities) {
            resultado.add(TipoIdentificacionDomain.reconstruir(
                    entity.getId(),
                    entity.getTipoIdentificacion(),
                    entity.getNombre()
            ));
        }

        return resultado;
    }
}
