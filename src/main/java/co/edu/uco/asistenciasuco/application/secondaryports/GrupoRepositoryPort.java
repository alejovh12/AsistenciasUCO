package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.GrupoRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;

import java.util.List;

/**
 * Puerto secundario para persistencia relacionada con grupos.
 */
public interface GrupoRepositoryPort {

    RegistrarEstudianteRepositoryEntity registrarEstudianteEnGrupo(RegistrarEstudianteRepositoryDTO dto);

    List<GrupoRepositoryEntity> consultarGrupos();
}
