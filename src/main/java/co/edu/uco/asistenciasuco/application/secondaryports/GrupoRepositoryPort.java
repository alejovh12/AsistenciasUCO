package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;

/**
 * Puerto secundario para persistencia relacionada con grupos.
 */
public interface GrupoRepositoryPort {

    void registrarEstudianteEnGrupo(RegistrarEstudianteRepositoryDTO dto);
}
