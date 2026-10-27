package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;

public interface ConsultarBitacoraEstudianteInputPort {

    BitacoraPaginaDTO execute(ConsultarBitacoraEstudianteDTO dto);
}
