package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.List;
import java.util.UUID;

/**
 * {@code franjas} (F1): si no es {@code null}, el repositorio guarda el horario en la misma transaccion que crea el grupo.
 */
public record CrearGrupoRepositoryDTO(
        UUID idGrupo,
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        UUID usuarioEjecutor,
        List<FranjaHorarioRepositoryDTO> franjas
) {

    public CrearGrupoRepositoryDTO(
            final UUID idGrupo,
            final UUID idAsignatura,
            final UUID idPeriodoAcademico,
            final Integer codigo,
            final String nombre,
            final UUID idDocente,
            final UUID usuarioEjecutor
    ) {
        this(idGrupo, idAsignatura, idPeriodoAcademico, codigo, nombre, idDocente, usuarioEjecutor, null);
    }
}
