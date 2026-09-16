package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

/**
 * Puerto de entrada para consultar las sesiones de un grupo.
 */
public interface ConsultarSesionesPorGrupoInputPort
        extends InteractorWithReturn<ConsultarSesionesPorGrupoDTO, List<SesionConsultadaDTO>> {
}
