package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;

import java.util.List;

public interface DecanoQueryPort {

    List<DecanoProjection> consultarDecanos();
}
