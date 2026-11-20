package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;

import java.util.List;
import java.util.UUID;

/** Horario propuesto de un grupo (SES-GEN-01). {@code idGrupo} nulo = grupo nuevo. */
public record PrevisualizarHorarioGrupoDomain(
        UUID idGrupo,
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        UUID idDocente,
        List<FranjaHorarioDomain> franjas,
        UUID usuarioEjecutor
) {
}
