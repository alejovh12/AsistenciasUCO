package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;

public interface ConsultarReclamosEstudianteInputPort {

    ReclamosEstudiantePaginaDTO execute(ConsultarReclamosEstudianteDTO dto);
}
