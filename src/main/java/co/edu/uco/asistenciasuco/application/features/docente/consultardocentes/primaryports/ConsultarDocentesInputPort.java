package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithoutInputWithReturn;

import java.util.List;

public interface ConsultarDocentesInputPort extends InteractorWithoutInputWithReturn<List<DocenteIdentidadDTO>> {
}
