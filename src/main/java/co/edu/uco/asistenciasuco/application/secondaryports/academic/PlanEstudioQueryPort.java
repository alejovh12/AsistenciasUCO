package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;

import java.util.List;
import java.util.UUID;

public interface PlanEstudioQueryPort {

    List<PlanEstudioProjection> consultarPlanesPorPrograma(UUID idPrograma);

    /** Planes de todos los programas que pertenecen a la facultad indicada (portal del decano). */
    List<PlanEstudioProjection> consultarPlanesPorFacultad(UUID idFacultad);
}
