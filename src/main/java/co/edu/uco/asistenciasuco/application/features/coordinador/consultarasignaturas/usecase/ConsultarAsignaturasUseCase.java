package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.domain.AsignaturaDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarAsignaturasUseCase { List<AsignaturaDomain> execute(UUID actorUsuarioId); }
