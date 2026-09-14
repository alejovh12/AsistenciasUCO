package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.mapper.ResolverSolicitudRevisionAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;

import java.util.Objects;

public final class ResolverSolicitudRevisionAsistenciaInteractor implements ResolverSolicitudRevisionAsistenciaInputPort {

    private final ResolverSolicitudRevisionAsistenciaUseCase useCase;

    public ResolverSolicitudRevisionAsistenciaInteractor(final ResolverSolicitudRevisionAsistenciaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ResolverSolicitudRevisionAsistenciaUseCase es obligatorio.");
    }

    @Override
    public void execute(final ResolverSolicitudRevisionAsistenciaDTO dto) {
        useCase.execute(ResolverSolicitudRevisionAsistenciaMapper.toDomain(dto));
    }
}
