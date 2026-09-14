package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultadQueryPort {

    List<FacultadProjection> consultarFacultades();

    Optional<FacultadProjection> consultarFacultadPorId(UUID idFacultad);
}
