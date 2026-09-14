package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;

import java.util.List; import java.util.UUID;

public interface ConsultarAsignaturasPlanUseCase { List<AsignaturaDomain> execute(UUID actorUsuarioId, UUID idPlanEstudio); }
