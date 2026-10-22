package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;

import java.util.List;
import java.util.UUID;

public interface AsignaturaQueryPort {

    List<AsignaturaProjection> consultarAsignaturasPorPrograma(UUID idPrograma);

    /** Asignaturas de los planes de todos los programas de la facultad indicada (portal del decano). */
    List<AsignaturaProjection> consultarAsignaturasPorFacultad(UUID idFacultad);

    List<AsignaturaProjection> consultarAsignaturasPorPlan(UUID idPlanEstudio);
}
