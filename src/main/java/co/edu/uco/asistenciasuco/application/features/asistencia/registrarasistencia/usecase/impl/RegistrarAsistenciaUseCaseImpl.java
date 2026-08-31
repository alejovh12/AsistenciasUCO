package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.RegistrarAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.mapper.RegistrarAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso registrar asistencia.
 */
public final class RegistrarAsistenciaUseCaseImpl implements RegistrarAsistenciaUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;

    public RegistrarAsistenciaUseCaseImpl(final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "El puerto de salida AsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }
        asistenciaRepositoryPort.registrarAsistencia(RegistrarAsistenciaRepositoryMapper.toRepositoryDTO(domain));
    }
}