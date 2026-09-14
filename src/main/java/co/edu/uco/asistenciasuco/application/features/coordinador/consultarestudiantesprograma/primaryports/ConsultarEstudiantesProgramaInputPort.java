package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarEstudiantesProgramaInputPort { List<EstudianteProgramaDTO> execute(UUID actorUsuarioId); }
