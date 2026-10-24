package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;

public interface ConsultarFichaEstudianteInputPort {

    FichaEstudianteDTO execute(ConsultarFichaEstudianteDTO dto);
}
