package co.edu.uco.asistenciasuco.application.secondaryports.report;

import java.time.LocalDateTime;

public record ReporteAsistenciaRow(
        String codigoGrupo,
        String nombreGrupo,
        Integer numeroSesion,
        String nombreSesion,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String documentoEstudiante,
        String nombreEstudiante,
        String correoEstudiante,
        Boolean asistio,
        String razonCausa
) {
}
