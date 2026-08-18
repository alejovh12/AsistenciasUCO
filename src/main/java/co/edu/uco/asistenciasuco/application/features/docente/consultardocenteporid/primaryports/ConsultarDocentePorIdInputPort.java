package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

public interface ConsultarDocentePorIdInputPort
        extends InteractorWithReturn<ConsultarDocentePorIdDTO, DocenteIdentidadDTO> {
}
