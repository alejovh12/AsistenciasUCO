package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;

import java.util.List;
import java.util.UUID;

public interface EstudianteProgramaQueryPort {

    List<EstudianteProgramaProjection> consultarEstudiantesPorPrograma(UUID idPrograma);
}
