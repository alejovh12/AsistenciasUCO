package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import java.util.List;

public interface ConsultarComparativaFacultadesInputPort {

    List<ComparativaFacultadDTO> execute(ConsultarComparativaFacultadesDTO dto);
}
