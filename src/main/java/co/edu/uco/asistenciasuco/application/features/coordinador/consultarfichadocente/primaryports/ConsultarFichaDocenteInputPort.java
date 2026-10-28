package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;

public interface ConsultarFichaDocenteInputPort {

    FichaDocenteDTO execute(ConsultarFichaDocenteDTO dto);
}
