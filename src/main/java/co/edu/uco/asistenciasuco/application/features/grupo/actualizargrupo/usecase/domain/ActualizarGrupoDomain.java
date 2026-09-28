package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain;

import java.util.UUID;

public record ActualizarGrupoDomain(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        UUID usuarioEjecutor
) {
}
