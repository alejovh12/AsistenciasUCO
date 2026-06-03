package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.AsistenciaRepositoryEntity;

import java.util.List;

/**
 * Puerto secundario para persistencia relacionada con asistencias.
 */
public interface AsistenciaRepositoryPort {

    void registrarAsistencia(RegistrarAsistenciaRepositoryDTO dto);

    List<AsistenciaRepositoryEntity> consultarAsistenciasPorGrupo(ConsultarAsistenciasPorGrupoRepositoryDTO dto);

    void solicitarRevisionAsistencia(SolicitarRevisionAsistenciaRepositoryDTO dto);
}
