package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;

import java.util.List;
import java.util.UUID;

/**
 * {@code franjas} (F1): horario semanal del grupo; {@code null} = el grupo se crea sin horario.
 */
public record CrearGrupoDTO(
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Boolean generarSesionesAutomaticas,
        UUID usuarioEjecutor,
        List<FranjaHorarioDTO> franjas
) {

    public CrearGrupoDTO(
            final UUID idAsignatura,
            final UUID idPeriodoAcademico,
            final Integer codigo,
            final String nombre,
            final UUID idDocente,
            final Boolean generarSesionesAutomaticas,
            final UUID usuarioEjecutor
    ) {
        this(idAsignatura, idPeriodoAcademico, codigo, nombre, idDocente, generarSesionesAutomaticas, usuarioEjecutor, null);
    }
}
