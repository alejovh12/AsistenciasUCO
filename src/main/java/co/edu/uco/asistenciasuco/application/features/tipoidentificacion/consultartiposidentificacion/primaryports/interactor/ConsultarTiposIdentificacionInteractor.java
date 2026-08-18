package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.mapper.ConsultarTiposIdentificacionMapper;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Interactor del puerto de entrada para consultar tipos de identificacion.
 */
public final class ConsultarTiposIdentificacionInteractor implements ConsultarTiposIdentificacionInputPort {

    private final ConsultarTiposIdentificacionUseCase useCase;

    public ConsultarTiposIdentificacionInteractor(final ConsultarTiposIdentificacionUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarTiposIdentificacionUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public List<TipoIdentificacionDTO> execute() {
        final List<TipoIdentificacionDomain> resultado = useCase.execute();
        return ConsultarTiposIdentificacionMapper.toDTOs(resultado);
    }
}
