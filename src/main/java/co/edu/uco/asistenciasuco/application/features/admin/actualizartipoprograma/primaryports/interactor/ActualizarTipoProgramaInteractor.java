package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.ActualizarTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.mapper.ActualizarTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.ActualizarTipoProgramaUseCase;

import java.util.Objects;

public class ActualizarTipoProgramaInteractor implements ActualizarTipoProgramaInputPort {

    private final ActualizarTipoProgramaUseCase useCase;

    public ActualizarTipoProgramaInteractor(final ActualizarTipoProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final ActualizarTipoProgramaDTO dto) {
        useCase.execute(ActualizarTipoProgramaMapper.toDomain(dto));
    }
}
