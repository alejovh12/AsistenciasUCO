package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.mapper.SolicitarRevisionAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Implementacion del caso de uso solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaUseCaseImpl implements SolicitarRevisionAsistenciaUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;

    public SolicitarRevisionAsistenciaUseCaseImpl(final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        if (ObjectHelper.isNull(asistenciaRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida AsistenciaRepositoryPort es obligatorio.");
        }
        this.asistenciaRepositoryPort = asistenciaRepositoryPort;
    }

    @Override
    public void execute(final SolicitarRevisionAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }
        asistenciaRepositoryPort.solicitarRevisionAsistencia(
                SolicitarRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain)
        );
    }
}
