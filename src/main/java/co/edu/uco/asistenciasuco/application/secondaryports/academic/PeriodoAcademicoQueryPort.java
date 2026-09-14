package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodoAcademicoQueryPort {

    List<PeriodoAcademicoProjection> consultarPeriodosAcademicos();

    Optional<PeriodoAcademicoProjection> consultarPeriodoAcademicoPorId(UUID idPeriodoAcademico);
}
