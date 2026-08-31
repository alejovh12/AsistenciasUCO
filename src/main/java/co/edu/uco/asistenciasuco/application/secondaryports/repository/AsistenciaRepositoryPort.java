package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;

import java.util.List;

/**
 * Puerto secundario para persistencia relacionada con asistencias.
 */
public interface AsistenciaRepositoryPort {

    void registrarAsistencia(RegistrarAsistenciaRepositoryDTO dto);

    List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(ConsultarAsistenciasPorGrupoRepositoryDTO dto);

    void solicitarRevisionAsistencia(SolicitarRevisionAsistenciaRepositoryDTO dto);
}
