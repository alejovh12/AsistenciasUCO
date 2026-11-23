package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity;

import java.time.LocalDateTime;

/**
 * Resultado interno de una fila del reporte de asistencia por grupo.
 */
public record ReporteAsistenciaEntity(
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
    public ReporteAsistenciaEntity(String codigoGrupo, String nombreGrupo, Integer numeroSesion, String nombreSesion,
                                   LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, String documentoEstudiante,
                                   String nombreEstudiante, String correoEstudiante, Boolean asistio, String razonCausa) {
        this(codigoGrupo, nombreGrupo, numeroSesion, nombreSesion, fechaHoraInicio, fechaHoraFin, documentoEstudiante,
                nombreEstudiante, correoEstudiante, asistio, razonCausa, false);
    }
}
