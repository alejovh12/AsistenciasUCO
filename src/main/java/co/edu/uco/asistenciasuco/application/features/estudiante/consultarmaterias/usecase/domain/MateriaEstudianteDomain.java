package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de materias del estudiante. CULM-001 B.2 (F-EST-002): incluye los
 * indicadores de asistencia por horas de la matricula.
 */
public record MateriaEstudianteDomain(UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo,
                                      IndicadoresAsistenciaDTO indicadores, Integer creditos) {

    public MateriaEstudianteDomain(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                   final String nombreGrupo, final IndicadoresAsistenciaDTO indicadores) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, indicadores, null);
    }

    public MateriaEstudianteDomain(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                   final String nombreGrupo) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, IndicadoresAsistenciaDTO.sinSesiones(null, null));
    }
}
