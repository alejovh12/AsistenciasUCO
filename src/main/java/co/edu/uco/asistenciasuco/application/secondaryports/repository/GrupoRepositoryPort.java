package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;

import java.util.List;

/**
 * Puerto secundario para persistencia relacionada con grupos.
 */
public interface GrupoRepositoryPort {

    RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(RegistrarEstudianteRepositoryDTO dto);

    List<GrupoRepositoryProjection> consultarGrupos();
}
