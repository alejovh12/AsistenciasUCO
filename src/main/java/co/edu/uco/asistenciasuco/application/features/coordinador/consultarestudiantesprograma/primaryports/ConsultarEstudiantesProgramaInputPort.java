package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.ConsultarEstudiantesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.PaginaEstudiantesProgramaDTO;

public interface ConsultarEstudiantesProgramaInputPort {
    PaginaEstudiantesProgramaDTO execute(ConsultarEstudiantesProgramaDTO consulta);
}
