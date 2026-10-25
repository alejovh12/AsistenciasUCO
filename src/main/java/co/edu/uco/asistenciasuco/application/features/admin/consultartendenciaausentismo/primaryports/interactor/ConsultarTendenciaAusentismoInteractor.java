package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.mapper.ConsultarTendenciaAusentismoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.ConsultarTendenciaAusentismoUseCase;
import java.util.List;
import java.util.Objects;

public class ConsultarTendenciaAusentismoInteractor implements ConsultarTendenciaAusentismoInputPort {

    private final ConsultarTendenciaAusentismoUseCase useCase;

    public ConsultarTendenciaAusentismoInteractor(final ConsultarTendenciaAusentismoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<PuntoTendenciaDTO> execute(final ConsultarTendenciaAusentismoDTO dto) {
        return ConsultarTendenciaAusentismoMapper.toDto(useCase.execute(ConsultarTendenciaAusentismoMapper.toDomain(dto)));
    }
}
