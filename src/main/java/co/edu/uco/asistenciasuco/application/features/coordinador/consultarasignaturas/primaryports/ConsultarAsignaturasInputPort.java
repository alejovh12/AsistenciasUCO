package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarAsignaturasInputPort { List<AsignaturaDTO> execute(UUID actorUsuarioId); }
