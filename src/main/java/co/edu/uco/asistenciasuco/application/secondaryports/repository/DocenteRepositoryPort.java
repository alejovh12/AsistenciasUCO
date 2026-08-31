package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;

import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario para persistencia relacionada con docentes.
 */
public interface DocenteRepositoryPort {

    List<DocenteIdentidadRepositoryProjection> consultarDocentes();

    Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorId(ConsultarDocentePorIdRepositoryDTO dto);

    List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesAcademicas(
            ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
    );

    DocenteOperacionRepositoryProjection registrarDocenteDesdeUsuario(RegistrarDocenteDesdeUsuarioRepositoryDTO dto);

    DocenteOperacionRepositoryProjection asignarDocenteAGrupo(AsignarDocenteAGrupoRepositoryDTO dto);
}
