package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarAsignaturasDocenteInputPort { List<AsignaturaDocenteDTO> execute(UUID actorUsuarioId); }
