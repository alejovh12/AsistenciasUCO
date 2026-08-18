package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.EstudianteDetalleRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.EstudiantePaginaRepositoryEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con estudiantes.
 */
public interface EstudianteRepositoryPort {

    EstudiantePaginaRepositoryEntity consultarEstudiantes(ConsultarEstudiantesRepositoryDTO dto);

    Optional<EstudianteDetalleRepositoryEntity> consultarEstudiantePorId(UUID estudianteId);
}
