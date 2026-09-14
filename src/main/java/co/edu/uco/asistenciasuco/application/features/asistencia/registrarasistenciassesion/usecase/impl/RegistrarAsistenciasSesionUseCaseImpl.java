package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.mapper.RegistrarAsistenciasSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.Objects;

public final class RegistrarAsistenciasSesionUseCaseImpl implements RegistrarAsistenciasSesionUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;

    public RegistrarAsistenciasSesionUseCaseImpl(final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciasSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencias de sesion es obligatorio.");
        }
        asistenciaRepositoryPort.registrarAsistenciasSesion(
                RegistrarAsistenciasSesionRepositoryMapper.toRepositoryDTO(domain)
        );
    }
}
