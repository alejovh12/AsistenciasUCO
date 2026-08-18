package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteAsignacionAcademicaRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteOperacionRepositoryEntity;

import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario para persistencia relacionada con docentes.
 */
public interface DocenteRepositoryPort {

    List<DocenteIdentidadRepositoryEntity> consultarDocentes();

    Optional<DocenteIdentidadRepositoryEntity> consultarDocentePorId(ConsultarDocentePorIdRepositoryDTO dto);

    List<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignacionesAcademicas(
            ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
    );

    DocenteOperacionRepositoryEntity registrarDocenteDesdeUsuario(RegistrarDocenteDesdeUsuarioRepositoryDTO dto);

    DocenteOperacionRepositoryEntity asignarDocenteAGrupo(AsignarDocenteAGrupoRepositoryDTO dto);
}
