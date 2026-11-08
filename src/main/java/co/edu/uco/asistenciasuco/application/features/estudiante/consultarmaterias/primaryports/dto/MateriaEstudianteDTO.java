package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;

import java.util.UUID;

/**
 * CULM-001 B.2 (F-EST-002, contrato 3.2.8): materia del estudiante con sus indicadores por horas (nunca nulos). CULM-001 D.9-a
 * (F-EST-006): incluye los creditos de la asignatura.
 */
public record MateriaEstudianteDTO(UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo,
                                   IndicadoresAsistenciaDTO indicadores, Integer creditos) {

    public MateriaEstudianteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                final String nombreGrupo, final IndicadoresAsistenciaDTO indicadores) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, indicadores, null);
    }

    public MateriaEstudianteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                final String nombreGrupo) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, IndicadoresAsistenciaDTO.sinSesiones(null, null));
    }
}
