package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;

import java.util.List;
import java.util.UUID;

/**
 * {@code franjas} (F1): horario semanal nuevo del grupo (reemplaza el anterior); {@code null} = el horario no cambia.
 */
public record ActualizarGrupoDTO(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        UUID usuarioEjecutor,
        List<FranjaHorarioDTO> franjas
) {

    public ActualizarGrupoDTO(
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
