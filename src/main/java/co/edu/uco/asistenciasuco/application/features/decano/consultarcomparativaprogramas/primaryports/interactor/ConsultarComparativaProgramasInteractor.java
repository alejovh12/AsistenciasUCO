package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.mapper.ConsultarComparativaProgramasMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.ConsultarComparativaProgramasUseCase;
import java.util.List;
import java.util.Objects;

public class ConsultarComparativaProgramasInteractor implements ConsultarComparativaProgramasInputPort {

    private final ConsultarComparativaProgramasUseCase useCase;

    public ConsultarComparativaProgramasInteractor(final ConsultarComparativaProgramasUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<ComparativaProgramaDTO> execute(final ConsultarComparativaProgramasDTO dto) {
        return ConsultarComparativaProgramasMapper.toDto(useCase.execute(ConsultarComparativaProgramasMapper.toDomain(dto)));
    }
}
