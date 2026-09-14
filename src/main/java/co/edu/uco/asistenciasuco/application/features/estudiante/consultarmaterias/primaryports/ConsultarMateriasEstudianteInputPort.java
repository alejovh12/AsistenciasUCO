package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarMateriasEstudianteInputPort { List<MateriaEstudianteDTO> execute(UUID actorUsuarioId); }
