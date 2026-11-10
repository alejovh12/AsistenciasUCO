package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CULM-001 B.2 (F-EST-002, contrato 3.2.8): materia del estudiante con sus indicadores por horas (nunca nulos). CULM-001 D.9-a
 * (F-EST-006): incluye los creditos de la asignatura. HU019: {@code horasProgramadas} y {@code sesionesProgramadas} son la
 * carga horaria total del grupo (sesiones no canceladas, dictadas o por dictar), para comparar con las horas dictadas y
 * asistidas de {@code indicadores}; nulos si no se conocen. El cliente no las calcula.
 */
public record MateriaEstudianteDTO(UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo,
                                   IndicadoresAsistenciaDTO indicadores, Integer creditos,
                                   BigDecimal horasProgramadas, Integer sesionesProgramadas) {

    /** Sin carga horaria programada. */
    public MateriaEstudianteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                final String nombreGrupo, final IndicadoresAsistenciaDTO indicadores, final Integer creditos) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, indicadores, creditos, null, null);
    }

    public MateriaEstudianteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                final String nombreGrupo, final IndicadoresAsistenciaDTO indicadores) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, indicadores, null);
    }

    public MateriaEstudianteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo,
                                final String nombreGrupo) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, IndicadoresAsistenciaDTO.sinSesiones(null, null));
    }
}
