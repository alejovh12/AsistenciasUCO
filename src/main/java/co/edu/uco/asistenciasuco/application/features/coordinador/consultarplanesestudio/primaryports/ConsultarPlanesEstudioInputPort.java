package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PlanEstudioDTO;
import java.util.List;
import java.util.UUID;

public interface ConsultarPlanesEstudioInputPort { List<PlanEstudioDTO> execute(UUID actorUsuarioId); }
