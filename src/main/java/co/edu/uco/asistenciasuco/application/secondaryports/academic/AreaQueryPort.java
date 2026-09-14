package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;

import java.util.List;

public interface AreaQueryPort {

    List<AreaProjection> consultarAreas();
}
