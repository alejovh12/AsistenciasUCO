package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;

import java.util.List; import java.util.UUID;

public interface ConsultarPlanesEstudioUseCase { List<PlanEstudioDomain> execute(UUID actorUsuarioId); }
