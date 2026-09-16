package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record ActualizarGrupoRepositoryDTO(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        String aula,
        UUID usuarioEjecutor
) {
}
