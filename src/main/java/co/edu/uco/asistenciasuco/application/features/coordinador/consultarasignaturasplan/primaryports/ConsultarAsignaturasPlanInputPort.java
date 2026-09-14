package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarAsignaturasPlanInputPort { List<AsignaturaDTO> execute(UUID actorUsuarioId, UUID idPlanEstudio); }
