package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;

import java.util.List;
import java.util.UUID;

public interface AsignaturaDocenteQueryPort {

    List<AsignaturaDocenteProjection> consultarAsignaturasDocente(UUID idDocente);
}
