package co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.ConsultarGruposDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.ConsultarGruposDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.mapper.ConsultarGruposMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarGruposDecanoInteractor implements ConsultarGruposDecanoInputPort {

    private final ConsultarGruposDecanoUseCase useCase;

    public ConsultarGruposDecanoInteractor(final ConsultarGruposDecanoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarGruposDecanoUseCase es obligatorio.");
    }

    @Override
    public List<GrupoDTO> execute(final UUID actorUsuarioId) {
        return ConsultarGruposMapper.toDTOs(useCase.execute(actorUsuarioId));
    }
}
