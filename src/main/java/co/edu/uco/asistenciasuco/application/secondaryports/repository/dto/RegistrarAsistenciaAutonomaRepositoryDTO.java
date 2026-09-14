package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record RegistrarAsistenciaAutonomaRepositoryDTO(
        UUID estudiante,
        UUID sesion,
        String codigoVerificacion
) {
}
