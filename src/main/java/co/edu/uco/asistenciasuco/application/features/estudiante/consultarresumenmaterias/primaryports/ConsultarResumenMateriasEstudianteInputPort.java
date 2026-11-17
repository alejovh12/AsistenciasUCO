package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;

import java.util.UUID;

public interface ConsultarResumenMateriasEstudianteInputPort {

    ResumenMateriasEstudianteDTO execute(UUID actorUsuarioId);
}
