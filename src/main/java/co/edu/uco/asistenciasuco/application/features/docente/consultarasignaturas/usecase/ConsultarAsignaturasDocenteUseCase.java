package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain.AsignaturaDocenteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarAsignaturasDocenteUseCase { List<AsignaturaDocenteDomain> execute(UUID actorUsuarioId); }
