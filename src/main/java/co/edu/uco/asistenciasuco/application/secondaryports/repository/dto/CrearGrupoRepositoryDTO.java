package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record CrearGrupoRepositoryDTO(
        UUID idGrupo,
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        String aula,
        UUID usuarioEjecutor
) {
}
