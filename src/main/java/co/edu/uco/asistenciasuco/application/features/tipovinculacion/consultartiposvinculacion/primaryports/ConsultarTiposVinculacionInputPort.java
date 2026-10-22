package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.dto.TipoVinculacionDTO;

import java.util.List;

public interface ConsultarTiposVinculacionInputPort {

    List<TipoVinculacionDTO> execute();
}
