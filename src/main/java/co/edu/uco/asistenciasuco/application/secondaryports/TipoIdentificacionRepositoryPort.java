package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.TipoIdentificacionRepositoryEntity;

import java.util.List;

/**
 * Puerto secundario para consulta del catalogo de tipos de identificacion.
 */
public interface TipoIdentificacionRepositoryPort {

    List<TipoIdentificacionRepositoryEntity> consultarTiposIdentificacion();
}
