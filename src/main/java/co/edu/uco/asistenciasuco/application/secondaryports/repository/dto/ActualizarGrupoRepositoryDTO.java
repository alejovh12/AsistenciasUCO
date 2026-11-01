package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.List;
import java.util.UUID;

/**
 * {@code franjas} (F1): si no es {@code null}, el repositorio reemplaza el horario en la misma transaccion que actualiza el grupo.
 */
public record ActualizarGrupoRepositoryDTO(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        UUID usuarioEjecutor,
        List<FranjaHorarioRepositoryDTO> franjas
) {

    public ActualizarGrupoRepositoryDTO(
            final UUID idGrupo,
            final Integer codigo,
            final String nombre,
            final UUID idDocente,
            final Integer cupoMaximo,
            final UUID usuarioEjecutor
    ) {
        this(idGrupo, codigo, nombre, idDocente, cupoMaximo, usuarioEjecutor, null);
    }
}
