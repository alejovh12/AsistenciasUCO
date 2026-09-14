package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;

import java.util.List;
import java.util.UUID;

public interface HorarioEstudianteQueryPort {

    List<HorarioEstudianteProjection> consultarHorarioEstudiante(UUID idEstudiante);
}
