package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.SolicitarIngresoGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto.SolicitarIngresoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.SolicitarIngresoGrupoUseCase;

import java.util.Objects;
import java.util.UUID;

public final class SolicitarIngresoGrupoInteractor implements SolicitarIngresoGrupoInputPort {

    private final SolicitarIngresoGrupoUseCase useCase;

    public SolicitarIngresoGrupoInteractor(final SolicitarIngresoGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public UUID execute(final SolicitarIngresoGrupoDTO dto) {
        if (dto == null || dto.actorUsuarioId() == null || dto.grupoId() == null) {
            throw new ValidationException("El usuario autenticado y el grupo son obligatorios.");
        }
        return useCase.execute(dto.actorUsuarioId(), dto.grupoId());
    }
}
