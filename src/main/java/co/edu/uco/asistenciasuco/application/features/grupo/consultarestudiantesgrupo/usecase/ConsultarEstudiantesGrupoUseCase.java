package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;

import java.util.List;

public interface ConsultarEstudiantesGrupoUseCase {

    List<EstudianteGrupoEntity> execute(ConsultarEstudiantesGrupoDomain domain);
}
