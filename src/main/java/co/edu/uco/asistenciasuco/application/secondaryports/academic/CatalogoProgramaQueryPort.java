package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CatalogoProgramaProjection;
import java.util.List;

/** Catalogos activos usados por el formulario de programa (tipo de programa, modalidad y jornada). */
public interface CatalogoProgramaQueryPort {

    List<CatalogoProgramaProjection> consultarTiposPrograma();

    List<CatalogoProgramaProjection> consultarModalidades();

    List<CatalogoProgramaProjection> consultarJornadas();
}
