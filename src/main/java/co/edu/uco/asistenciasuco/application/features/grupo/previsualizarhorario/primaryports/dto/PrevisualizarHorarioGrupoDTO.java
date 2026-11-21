package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;

import java.util.List;
import java.util.UUID;

/** Entrada de la previsualizacion del horario de un grupo (SES-GEN-01). {@code idGrupo} nulo = grupo nuevo. */
public record PrevisualizarHorarioGrupoDTO(
        UUID idGrupo,
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        UUID idDocente,
        List<FranjaHorarioDTO> franjas,
        UUID usuarioEjecutor
) {
}
