package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.mapper.ConsultarTiposProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;

import java.util.List;
import java.util.Objects;

public class ConsultarTiposProgramaInteractor implements ConsultarTiposProgramaInputPort {

    private final ConsultarTiposProgramaUseCase useCase;

    public ConsultarTiposProgramaInteractor(final ConsultarTiposProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<TipoProgramaDTO> consultarTodos() {
        return useCase.execute(false).stream().map(ConsultarTiposProgramaMapper::toDTO).toList();
    }

    @Override
    public List<TipoProgramaDTO> consultarActivos() {
        return useCase.execute(true).stream().map(ConsultarTiposProgramaMapper::toDTO).toList();
    }
}
