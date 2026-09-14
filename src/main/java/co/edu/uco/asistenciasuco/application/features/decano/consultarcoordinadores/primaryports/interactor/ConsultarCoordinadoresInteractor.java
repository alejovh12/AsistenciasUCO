package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.mapper.ConsultarCoordinadoresMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarCoordinadoresInteractor implements ConsultarCoordinadoresInputPort {
    private final ConsultarCoordinadoresUseCase useCase;
    public ConsultarCoordinadoresInteractor(final ConsultarCoordinadoresUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<CoordinadorDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarCoordinadoresMapper::toDTO).toList();
    }
}
