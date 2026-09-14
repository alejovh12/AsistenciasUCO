package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteInputPort {
    List<SesionMateriaEstudianteDTO> execute(UUID actorUsuarioId, UUID idAsignatura);
}
