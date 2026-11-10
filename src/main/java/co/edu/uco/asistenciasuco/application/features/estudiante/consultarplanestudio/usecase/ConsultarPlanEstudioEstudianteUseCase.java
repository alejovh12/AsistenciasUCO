package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.PlanEstudioEstudianteDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarPlanEstudioEstudianteUseCase {

    List<PlanEstudioEstudianteDomain> execute(UUID actorUsuarioId);
}
