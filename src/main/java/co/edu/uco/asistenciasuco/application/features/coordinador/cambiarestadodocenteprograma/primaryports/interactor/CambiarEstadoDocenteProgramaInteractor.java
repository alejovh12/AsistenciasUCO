package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.CambiarEstadoDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.dto.CambiarEstadoDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.CambiarEstadoDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain.CambiarEstadoDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;

import java.util.Objects;

public final class CambiarEstadoDocenteProgramaInteractor implements CambiarEstadoDocenteProgramaInputPort {

    private final CambiarEstadoDocenteProgramaUseCase useCase;

    public CambiarEstadoDocenteProgramaInteractor(final CambiarEstadoDocenteProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public DocenteProgramaDTO execute(final CambiarEstadoDocenteProgramaDTO dto) {
        return DocenteProgramaMapper.toDTO(useCase.execute(
                new CambiarEstadoDocenteProgramaDomain(dto.actorUsuarioId(), dto.idDocente(), dto.estaActivo())));
    }
}
