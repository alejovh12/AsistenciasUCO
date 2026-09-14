package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteUseCase {
    List<SesionMateriaEstudianteDomain> execute(UUID actorUsuarioId, UUID idAsignatura);
}
