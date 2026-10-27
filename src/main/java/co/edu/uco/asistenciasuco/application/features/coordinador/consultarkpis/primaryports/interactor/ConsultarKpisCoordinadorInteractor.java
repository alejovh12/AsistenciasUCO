package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.mapper.ConsultarKpisCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.ConsultarKpisCoordinadorUseCase;
import java.util.Objects;

public class ConsultarKpisCoordinadorInteractor implements ConsultarKpisCoordinadorInputPort {

    private final ConsultarKpisCoordinadorUseCase useCase;

    public ConsultarKpisCoordinadorInteractor(final ConsultarKpisCoordinadorUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public KpisCoordinadorDTO execute(final ConsultarKpisCoordinadorDTO dto) {
        return ConsultarKpisCoordinadorMapper.toDto(useCase.execute(ConsultarKpisCoordinadorMapper.toDomain(dto)));
    }
}
