package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarPlanEstudioEstudianteInputPort {

    List<PlanEstudioEstudianteDTO> execute(UUID actorUsuarioId);
}
