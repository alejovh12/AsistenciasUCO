package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import java.util.List;

public interface ConsultarCatalogosProgramaInputPort {

    List<CatalogoProgramaDTO> tiposPrograma();

    List<CatalogoProgramaDTO> modalidades();

    List<CatalogoProgramaDTO> jornadas();
}
