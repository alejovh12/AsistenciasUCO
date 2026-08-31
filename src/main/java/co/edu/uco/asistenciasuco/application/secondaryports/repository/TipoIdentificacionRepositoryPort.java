package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;

import java.util.List;

/**
 * Puerto secundario para consulta del catalogo de tipos de identificacion.
 */
public interface TipoIdentificacionRepositoryPort {

    List<TipoIdentificacionRepositoryProjection> consultarTiposIdentificacion();
}
