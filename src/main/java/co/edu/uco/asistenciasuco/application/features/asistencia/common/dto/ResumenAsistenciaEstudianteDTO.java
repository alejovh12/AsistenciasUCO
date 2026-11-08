package co.edu.uco.asistenciasuco.application.features.asistencia.common.dto;

import java.util.UUID;

/** CULM-001 B.2 / C.1 (F-DOC-004): fila de la sabana del docente con los indicadores por horas de un estudiante del grupo. */
public record ResumenAsistenciaEstudianteDTO(
        UUID estudianteId,
        String nombreCompleto,
        String codigoEstadoEstudiante,
        IndicadoresAsistenciaDTO indicadores,
        MarcaInasistenciaDTO marca
) {

    /** Sin marca de inasistencia ({@code null}). */
    public ResumenAsistenciaEstudianteDTO(final UUID estudianteId, final String nombreCompleto, final String codigoEstadoEstudiante,
                                          final IndicadoresAsistenciaDTO indicadores) {
        this(estudianteId, nombreCompleto, codigoEstadoEstudiante, indicadores, null);
    }
}
