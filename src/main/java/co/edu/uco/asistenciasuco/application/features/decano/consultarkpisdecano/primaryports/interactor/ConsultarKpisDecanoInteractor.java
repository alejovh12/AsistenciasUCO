package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;

import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.mapper.ConsultarKpisDecanoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.ConsultarKpisDecanoUseCase;
import java.util.Objects;

public class ConsultarKpisDecanoInteractor implements ConsultarKpisDecanoInputPort {

    private final ConsultarKpisDecanoUseCase useCase;

    public ConsultarKpisDecanoInteractor(final ConsultarKpisDecanoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public KpisDecanoDTO execute(final ConsultarKpisDecanoDTO dto) {
        return ConsultarKpisDecanoMapper.toDto(useCase.execute(ConsultarKpisDecanoMapper.toDomain(dto)));
    }
}
