package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para consultar una sesion.
 */
public interface ConsultarSesionInputPort
        extends InteractorWithReturn<ConsultarSesionDTO, SesionConsultadaDTO> {
}
