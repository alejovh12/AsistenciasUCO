package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.ResolverSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResolverSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResultadoSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.ResolverSolicitudIngresoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.domain.ResolverSolicitudIngresoDomain;

import java.util.Objects;

public final class ResolverSolicitudIngresoInteractor implements ResolverSolicitudIngresoInputPort {

    private final ResolverSolicitudIngresoUseCase useCase;

    public ResolverSolicitudIngresoInteractor(final ResolverSolicitudIngresoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ResultadoSolicitudIngresoDTO execute(final ResolverSolicitudIngresoDTO dto) {
        final ResolverSolicitudIngresoDomain domain = ResolverSolicitudIngresoDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.solicitudId(),
                dto == null ? null : dto.accion(),
                dto == null ? null : dto.motivo());
        return new ResultadoSolicitudIngresoDTO(domain.solicitudId(), useCase.execute(domain));
    }
}
