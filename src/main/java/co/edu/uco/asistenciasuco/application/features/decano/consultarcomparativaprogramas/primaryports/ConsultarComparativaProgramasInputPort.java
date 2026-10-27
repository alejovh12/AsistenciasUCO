package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import java.util.List;

public interface ConsultarComparativaProgramasInputPort {

    List<ComparativaProgramaDTO> execute(ConsultarComparativaProgramasDTO dto);
}
