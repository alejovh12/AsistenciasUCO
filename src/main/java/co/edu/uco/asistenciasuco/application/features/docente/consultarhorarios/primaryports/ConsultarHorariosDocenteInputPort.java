package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarHorariosDocenteInputPort { List<HorarioDocenteDTO> execute(UUID actorUsuarioId); }
