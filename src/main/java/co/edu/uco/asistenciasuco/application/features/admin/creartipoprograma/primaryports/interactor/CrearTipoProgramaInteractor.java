package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.CrearTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto.CrearTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.mapper.CrearTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.CrearTipoProgramaUseCase;

import java.util.Objects;

public class CrearTipoProgramaInteractor implements CrearTipoProgramaInputPort {

    private final CrearTipoProgramaUseCase useCase;

    public CrearTipoProgramaInteractor(final CrearTipoProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final CrearTipoProgramaDTO dto) {
        useCase.execute(CrearTipoProgramaMapper.toDomain(dto));
    }
}
