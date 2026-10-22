package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.mapper.ConsultarProgramasMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import java.util.Objects;

public class ConsultarProgramasInteractor implements ConsultarProgramasInputPort {

    private final ConsultarProgramasUseCase useCase;

    public ConsultarProgramasInteractor(final ConsultarProgramasUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ProgramasPaginaDTO execute(final ConsultarProgramasDTO dto) {
        return ConsultarProgramasMapper.toPagina(useCase.execute(ConsultarProgramasMapper.toDomain(dto)));
    }
}
