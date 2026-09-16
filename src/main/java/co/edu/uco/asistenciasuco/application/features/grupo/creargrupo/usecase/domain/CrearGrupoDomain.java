package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain;

import java.util.UUID;

public record CrearGrupoDomain(
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        String aula,
        boolean generarSesionesAutomaticas,
        UUID usuarioEjecutor
) {
}
