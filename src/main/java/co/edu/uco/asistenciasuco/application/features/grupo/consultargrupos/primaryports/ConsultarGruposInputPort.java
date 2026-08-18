package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithoutInputWithReturn;

import java.util.List;

/**
 * Puerto de entrada para consultar grupos.
 */
public interface ConsultarGruposInputPort extends InteractorWithoutInputWithReturn<List<GrupoDTO>> {
}
