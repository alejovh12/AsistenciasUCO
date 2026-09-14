package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarMateriasEstudianteUseCase { List<MateriaEstudianteDomain> execute(UUID actorUsuarioId); }
