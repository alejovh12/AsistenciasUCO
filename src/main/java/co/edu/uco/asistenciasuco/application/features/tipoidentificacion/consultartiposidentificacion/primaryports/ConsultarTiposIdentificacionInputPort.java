package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithoutInputWithReturn;

import java.util.List;

/**
 * Puerto de entrada para consultar tipos de identificacion.
 */
public interface ConsultarTiposIdentificacionInputPort
        extends InteractorWithoutInputWithReturn<List<TipoIdentificacionDTO>> {
}
