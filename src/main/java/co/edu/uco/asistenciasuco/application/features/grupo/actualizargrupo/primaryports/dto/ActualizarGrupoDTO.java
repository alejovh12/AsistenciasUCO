package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto;

import java.util.UUID;

public record ActualizarGrupoDTO(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        String aula,
        UUID usuarioEjecutor
) {
}
