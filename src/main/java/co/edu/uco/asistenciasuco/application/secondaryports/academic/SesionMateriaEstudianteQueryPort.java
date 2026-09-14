package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

import java.util.List;
import java.util.UUID;

public interface SesionMateriaEstudianteQueryPort {

    List<SesionMateriaEstudianteProjection> consultarSesionesMateria(UUID idEstudiante, UUID idAsignatura);
}
