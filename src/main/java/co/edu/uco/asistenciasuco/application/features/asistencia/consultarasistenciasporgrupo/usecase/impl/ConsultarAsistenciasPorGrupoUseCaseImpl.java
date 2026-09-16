package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.mapper.ConsultarAsistenciasPorGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar asistencias por grupo.
 *
 * <p>Autorizacion: DOCENTE, COORDINADOR y ADMINISTRADOR pueden invocar este endpoint (ver
 * {@code SecurityConfig}). Coordinador/Administrador no resuelven a una identidad Docente y
 * conservan su visibilidad institucional; si el usuario autenticado SI resuelve a un Docente,
 * debe tener titularidad sobre el grupo consultado.</p>
 */
public final class ConsultarAsistenciasPorGrupoUseCaseImpl implements ConsultarAsistenciasPorGrupoUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarAsistenciasPorGrupoUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "El puerto de salida AsistenciaRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public List<AsistenciaConsultadaEntity> execute(final ConsultarAsistenciasPorGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        if (institutionalScopePort.findDocenteIdByUsuario(domain.getUsuarioEjecutor()).isPresent()
                && !institutionalScopePort.canDocenteAccessGrupo(domain.getUsuarioEjecutor(), domain.getGrupo())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo consultado.");
        }

        return ConsultarAsistenciasPorGrupoRepositoryMapper.toUseCaseEntities(
                asistenciaRepositoryPort.consultarAsistenciasPorGrupo(
                        ConsultarAsistenciasPorGrupoRepositoryMapper.toRepositoryDTO(domain)
                )
        );
    }
}