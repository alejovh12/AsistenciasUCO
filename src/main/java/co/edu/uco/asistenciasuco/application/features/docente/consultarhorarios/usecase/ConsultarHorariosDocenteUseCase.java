package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarHorariosDocenteUseCase { List<HorarioDocenteDomain> execute(UUID actorUsuarioId); }
