package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;

import java.util.List;
import java.util.UUID;

public record CrearGrupoDomain(
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        boolean generarSesionesAutomaticas,
        UUID usuarioEjecutor,
        List<FranjaHorarioDomain> franjas
) {

    public CrearGrupoDomain(
            final UUID idAsignatura,
            final UUID idPeriodoAcademico,
            final Integer codigo,
            final String nombre,
            final UUID idDocente,
            final boolean generarSesionesAutomaticas,
            final UUID usuarioEjecutor
    ) {
        this(idAsignatura, idPeriodoAcademico, codigo, nombre, idDocente, generarSesionesAutomaticas, usuarioEjecutor, null);
    }
}
