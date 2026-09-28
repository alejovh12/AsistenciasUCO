package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.List;
import java.util.UUID;

public record RegistrarAsistenciasSesionRepositoryDTO(
        UUID sesion,
        List<RegistroAsistenciaSesionRepositoryDTO> registros,
        UUID usuarioEjecutor
) {
}
