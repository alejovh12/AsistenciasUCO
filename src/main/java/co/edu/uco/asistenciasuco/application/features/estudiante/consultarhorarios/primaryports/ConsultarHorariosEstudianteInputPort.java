package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarHorariosEstudianteInputPort { List<HorarioEstudianteDTO> execute(UUID actorUsuarioId); }
