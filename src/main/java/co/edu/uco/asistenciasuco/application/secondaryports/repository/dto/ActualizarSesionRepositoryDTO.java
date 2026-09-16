package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActualizarSesionRepositoryDTO(
        UUID sesion,
        String nombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String aula,
        String descripcion,
        UUID docente,
        UUID usuarioEjecutor
) {
}
