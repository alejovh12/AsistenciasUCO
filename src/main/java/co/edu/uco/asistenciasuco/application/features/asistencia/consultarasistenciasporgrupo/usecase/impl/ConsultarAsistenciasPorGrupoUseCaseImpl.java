package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.mapper.ConsultarAsistenciasPorGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoUseCaseImpl implements ConsultarAsistenciasPorGrupoUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;

    public ConsultarAsistenciasPorGrupoUseCaseImpl(final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "El puerto de salida AsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public List<AsistenciaConsultadaEntity> execute(final ConsultarAsistenciasPorGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        return ConsultarAsistenciasPorGrupoRepositoryMapper.toUseCaseEntities(
                asistenciaRepositoryPort.consultarAsistenciasPorGrupo(
                        ConsultarAsistenciasPorGrupoRepositoryMapper.toRepositoryDTO(domain)
                )
        );
    }
}