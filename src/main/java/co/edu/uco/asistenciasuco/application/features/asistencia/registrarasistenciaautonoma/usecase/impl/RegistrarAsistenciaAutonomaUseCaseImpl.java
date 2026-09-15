package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.RegistrarAsistenciaAutonomaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain.RegistrarAsistenciaAutonomaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.mapper.RegistrarAsistenciaAutonomaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

public final class RegistrarAsistenciaAutonomaUseCaseImpl implements RegistrarAsistenciaAutonomaUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public RegistrarAsistenciaAutonomaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciaAutonomaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencia autonoma es obligatorio.");
        }
        final var estudianteId = institutionalScopePort.findEstudianteIdByUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        asistenciaRepositoryPort.registrarAsistenciaAutonoma(
                RegistrarAsistenciaAutonomaRepositoryMapper.toRepositoryDTO(domain, estudianteId)
        );
    }
}
