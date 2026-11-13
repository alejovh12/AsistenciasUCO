package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.mapper.ConsultarMateriasEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorasProgramadasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorasProgramadasEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Materias del estudiante autenticado. CULM-001 B.2 (F-EST-002, contrato 3.2.8): une por {@code idGrupo} la consulta de
 * materias con {@code usp_consultar_resumen_asistencia_estudiante} (una llamada); una materia sin fila de resumen recibe
 * indicadores en cero (nunca nulos) con los umbrales conocidos.
 *
 * <p>HU019: une tambien por {@code idGrupo} las horas programadas del grupo
 * ({@code usp_consultar_horas_programadas_estudiante}, una llamada). Una materia sin fila queda con la carga horaria en
 * nulo: nunca se sustituye por ceros ni se calcula aqui.</p>
 */
public final class ConsultarMateriasEstudianteUseCaseImpl implements ConsultarMateriasEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final MateriaEstudianteQueryPort queryPort;
    private final ResumenAsistenciaQueryPort resumenPort;
    private final HorasProgramadasEstudianteQueryPort horasProgramadasPort;

    /** Sin horas programadas: las materias salen con la carga horaria en nulo. */
    public ConsultarMateriasEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort,
                                                  final ResumenAsistenciaQueryPort resumenPort) {
        this(scopePort, queryPort, resumenPort, usuario -> List.of());
    }

    public ConsultarMateriasEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort,
                                                  final ResumenAsistenciaQueryPort resumenPort,
                                                  final HorasProgramadasEstudianteQueryPort horasProgramadasPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.resumenPort = Objects.requireNonNull(resumenPort);
        this.horasProgramadasPort = Objects.requireNonNull(horasProgramadasPort);
    }

    @Override
    public List<MateriaEstudianteDomain> execute(final UUID actorUsuarioId) {
        // CULM-001 A.2-11: sin estudiante, 403; el SP recibe el usuario y solo devuelve matriculas inscritas (A).
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_004));
        final List<MateriaEstudianteDomain> materias = queryPort.consultarMateriasEstudiante(actorUsuarioId).stream()
                .map(ConsultarMateriasEstudianteRepositoryMapper::toDomain)
                .toList();
        if (materias.isEmpty()) {
            return materias;
        }
        final Map<UUID, IndicadoresAsistenciaDTO> porGrupo = new HashMap<>();
        IndicadoresAsistenciaDTO referencia = null;
        for (final ResumenAsistenciaQueryPort.FilaResumen fila : resumenPort.consultarResumenEstudiante(actorUsuarioId)) {
            porGrupo.put(fila.idGrupo(), fila.indicadores());
            referencia = fila.indicadores();
        }
        final IndicadoresAsistenciaDTO vacio = referencia == null
                ? IndicadoresAsistenciaDTO.sinSesiones(null, null)
                : IndicadoresAsistenciaDTO.sinSesiones(referencia.umbralAdvertencia(), referencia.umbralPerdida());
        final Map<UUID, HorasProgramadasEstudianteProjection> programadas = new HashMap<>();
        for (final HorasProgramadasEstudianteProjection fila : horasProgramadasPort.consultarHorasProgramadas(actorUsuarioId)) {
            programadas.put(fila.idGrupo(), fila);
        }
        return materias.stream()
                .map(materia -> conIndicadoresYCarga(materia, porGrupo.getOrDefault(materia.idGrupo(), vacio),
                        programadas.get(materia.idGrupo())))
                .toList();
    }

    private static MateriaEstudianteDomain conIndicadoresYCarga(final MateriaEstudianteDomain materia,
                                                                final IndicadoresAsistenciaDTO indicadores,
                                                                final HorasProgramadasEstudianteProjection carga) {
        return new MateriaEstudianteDomain(materia.idAsignatura(), materia.nombreAsignatura(), materia.idGrupo(),
                materia.nombreGrupo(), indicadores, materia.creditos(),
                carga == null ? null : carga.horasProgramadas(),
                carga == null ? null : carga.sesionesProgramadas());
    }
}
