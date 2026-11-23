package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ActualizarSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ResultadoSemestreEstudianteDTO;

public interface ActualizarSemestreEstudianteInputPort {

    ResultadoSemestreEstudianteDTO execute(ActualizarSemestreEstudianteDTO dto);
}
