package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.domain.PrevisualizarHorarioGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;

public interface PrevisualizarHorarioGrupoUseCase {

    PrevisualizacionHorarioGrupoProjection execute(PrevisualizarHorarioGrupoDomain domain);
}
