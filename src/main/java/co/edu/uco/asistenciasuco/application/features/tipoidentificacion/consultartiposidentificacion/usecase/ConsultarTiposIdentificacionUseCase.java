package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithoutInputWithReturn;

import java.util.List;

/**
 * Caso de uso para consultar el catalogo de tipos de identificacion.
 */
public interface ConsultarTiposIdentificacionUseCase
        extends UseCaseWithoutInputWithReturn<List<TipoIdentificacionDomain>> {
}
