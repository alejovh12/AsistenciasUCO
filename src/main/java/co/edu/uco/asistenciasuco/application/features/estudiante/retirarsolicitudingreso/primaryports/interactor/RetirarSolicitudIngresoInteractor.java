package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.RetirarSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto.RetirarSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.RetirarSolicitudIngresoUseCase;

import java.util.Objects;

public final class RetirarSolicitudIngresoInteractor implements RetirarSolicitudIngresoInputPort {

    private final RetirarSolicitudIngresoUseCase useCase;

    public RetirarSolicitudIngresoInteractor(final RetirarSolicitudIngresoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final RetirarSolicitudIngresoDTO dto) {
        if (dto == null || dto.actorUsuarioId() == null || dto.solicitudId() == null) {
            throw new ValidationException("El usuario autenticado y la solicitud son obligatorios.");
        }
        useCase.execute(dto.actorUsuarioId(), dto.solicitudId());
    }
}
