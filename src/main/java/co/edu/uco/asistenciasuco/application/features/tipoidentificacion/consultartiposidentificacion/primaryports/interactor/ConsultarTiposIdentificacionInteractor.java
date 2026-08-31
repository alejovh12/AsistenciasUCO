package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.mapper.ConsultarTiposIdentificacionMapper;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar tipos de identificacion.
 */
public final class ConsultarTiposIdentificacionInteractor implements ConsultarTiposIdentificacionInputPort {

    private final ConsultarTiposIdentificacionUseCase useCase;

    public ConsultarTiposIdentificacionInteractor(final ConsultarTiposIdentificacionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarTiposIdentificacionUseCase es obligatorio.");
    }

    @Override
    public List<TipoIdentificacionDTO> execute() {
        final List<TipoIdentificacionDomain> resultado = useCase.execute();
        return ConsultarTiposIdentificacionMapper.toDTOs(resultado);
    }
}