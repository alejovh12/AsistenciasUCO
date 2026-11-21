package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizacionHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizarHorarioGrupoDTO;

/** SES-GEN-01: previsualiza, sin escribir, el calendario de sesiones de un horario propuesto para un grupo. */
public interface PrevisualizarHorarioGrupoInputPort {

    PrevisualizacionHorarioGrupoDTO execute(PrevisualizarHorarioGrupoDTO dto);
}
