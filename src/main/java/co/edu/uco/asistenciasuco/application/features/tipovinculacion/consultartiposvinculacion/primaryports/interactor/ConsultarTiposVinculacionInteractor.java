package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.ConsultarTiposVinculacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.dto.TipoVinculacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.ConsultarTiposVinculacionUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarTiposVinculacionInteractor implements ConsultarTiposVinculacionInputPort {

    private final ConsultarTiposVinculacionUseCase useCase;

    public ConsultarTiposVinculacionInteractor(final ConsultarTiposVinculacionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<TipoVinculacionDTO> execute() {
        return useCase.execute().stream().map(d -> new TipoVinculacionDTO(d.id(), d.codigo(), d.nombre())).toList();
    }
}
