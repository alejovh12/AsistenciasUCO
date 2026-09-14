package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;

import java.util.List;

public interface ParametroQueryPort {

    List<ParametroProjection> consultarParametros();
}
