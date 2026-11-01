package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;

import java.util.List;
import java.util.UUID;

public record ActualizarGrupoDomain(
        UUID idGrupo,
        Integer codigo,
        String nombre,
        UUID idDocente,
        Integer cupoMaximo,
        UUID usuarioEjecutor,
        List<FranjaHorarioDomain> franjas
) {

    public ActualizarGrupoDomain(
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
