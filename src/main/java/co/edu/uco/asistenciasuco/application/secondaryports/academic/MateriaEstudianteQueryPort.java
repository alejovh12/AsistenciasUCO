package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;

import java.util.List;
import java.util.UUID;

public interface MateriaEstudianteQueryPort {

    List<MateriaEstudianteProjection> consultarMateriasEstudiante(UUID idEstudiante);
}
