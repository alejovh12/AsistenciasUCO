package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public record SesionMateriaEstudianteProjection(
        UUID id,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        UUID idGrupo,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin
) {
}
