package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record ResolverSolicitudRevisionAsistenciaRepositoryDTO(
        UUID solicitud,
        UUID docente,
        String accion,
        String respuestaDocente,
        UUID usuarioEjecutor
) {
}
