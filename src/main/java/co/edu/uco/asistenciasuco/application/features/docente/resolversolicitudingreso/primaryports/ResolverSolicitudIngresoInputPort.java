package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResolverSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResultadoSolicitudIngresoDTO;

public interface ResolverSolicitudIngresoInputPort {

    ResultadoSolicitudIngresoDTO execute(ResolverSolicitudIngresoDTO dto);
}
