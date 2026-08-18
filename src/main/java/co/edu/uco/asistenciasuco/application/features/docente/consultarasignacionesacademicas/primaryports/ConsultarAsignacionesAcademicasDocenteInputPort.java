package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

public interface ConsultarAsignacionesAcademicasDocenteInputPort
        extends InteractorWithReturn<ConsultarAsignacionesAcademicasDocenteDTO, List<DocenteAsignacionAcademicaDTO>> {
}
