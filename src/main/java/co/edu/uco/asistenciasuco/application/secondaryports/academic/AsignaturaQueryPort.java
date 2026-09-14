package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;

import java.util.List;
import java.util.UUID;

public interface AsignaturaQueryPort {

    List<AsignaturaProjection> consultarAsignaturasPorPrograma(UUID idPrograma);

    List<AsignaturaProjection> consultarAsignaturasPorPlan(UUID idPlanEstudio);
}
