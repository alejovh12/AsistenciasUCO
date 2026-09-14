package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;

import java.util.List;

public interface InstitucionQueryPort {

    List<InstitucionProjection> consultarInstituciones();
}
