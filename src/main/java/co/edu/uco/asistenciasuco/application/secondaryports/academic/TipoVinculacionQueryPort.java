package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoVinculacionProjection;

import java.util.List;

/** Puerto de salida del catalogo de tipos de vinculacion (HU090). */
public interface TipoVinculacionQueryPort {

    List<TipoVinculacionProjection> consultarActivos();
}
