package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.domain.ResumenMateriasEstudianteDomain;

import java.util.UUID;

public interface ConsultarResumenMateriasEstudianteUseCase {

    ResumenMateriasEstudianteDomain execute(UUID actorUsuarioId);
}
