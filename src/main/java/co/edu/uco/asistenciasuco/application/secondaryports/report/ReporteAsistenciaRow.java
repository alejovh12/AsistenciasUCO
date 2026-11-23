package co.edu.uco.asistenciasuco.application.secondaryports.report;

import java.time.LocalDateTime;

/** Fila del reporte. Las fechas son el reloj UTC de la base; {@code sesionCancelada} marca las sesiones con SesionCancelacion. */
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
        String razonCausa,
        boolean sesionCancelada
) {
    /** Fila de una sesion no cancelada. */
    public ReporteAsistenciaRow(String codigoGrupo, String nombreGrupo, Integer numeroSesion, String nombreSesion,
                                LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, String documentoEstudiante,
                                String nombreEstudiante, String correoEstudiante, Boolean asistio, String razonCausa) {
        this(codigoGrupo, nombreGrupo, numeroSesion, nombreSesion, fechaHoraInicio, fechaHoraFin, documentoEstudiante,
                nombreEstudiante, correoEstudiante, asistio, razonCausa, false);
    }
}
