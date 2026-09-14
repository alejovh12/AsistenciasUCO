package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarHorariosEstudianteUseCase { List<HorarioEstudianteDomain> execute(UUID actorUsuarioId); }
