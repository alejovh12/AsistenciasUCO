package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoDocentePaginaDTO;

public interface ConsultarSolicitudesIngresoDocenteInputPort {

    SolicitudesIngresoDocentePaginaDTO execute(ConsultarSolicitudesIngresoDocenteDTO dto);
}
