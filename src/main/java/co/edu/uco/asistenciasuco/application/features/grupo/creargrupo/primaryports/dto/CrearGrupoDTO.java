package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto;

import java.util.UUID;

public record CrearGrupoDTO(
        UUID idAsignatura,
        UUID idPeriodoAcademico,
        Integer codigo,
        String nombre,
        UUID idDocente,
        String aula,
        Boolean generarSesionesAutomaticas,
        UUID usuarioEjecutor
) {
}
