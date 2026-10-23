package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.ConsultarCandidatosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.ConsultarCandidatosDocenteUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarCandidatosDocenteInteractor implements ConsultarCandidatosDocenteInputPort {

    private final ConsultarCandidatosDocenteUseCase useCase;

    public ConsultarCandidatosDocenteInteractor(final ConsultarCandidatosDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<DocenteCandidatoDTO> execute(final UUID actorUsuarioId, final String busqueda) {
        return useCase.execute(actorUsuarioId, busqueda).stream().map(DocenteProgramaMapper::toDTO).toList();
    }
}
