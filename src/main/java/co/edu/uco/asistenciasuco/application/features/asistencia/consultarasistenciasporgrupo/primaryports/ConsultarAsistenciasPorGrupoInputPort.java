package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

/**
 * Puerto de entrada para consultar asistencias por grupo.
 */
public interface ConsultarAsistenciasPorGrupoInputPort
        extends InteractorWithReturn<ConsultarAsistenciasPorGrupoDTO, List<AsistenciaConsultadaDTO>> {
}
