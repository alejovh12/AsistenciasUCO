package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto;

import java.time.LocalDateTime;

public record ReporteAsistenciaDTO(String codigoGrupo, String nombreGrupo, Integer numeroSesion, String nombreSesion,
                                   LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                   String documentoEstudiante, String nombreEstudiante, String correoEstudiante,
                                   Boolean asistio, String razonCausa) {
}
