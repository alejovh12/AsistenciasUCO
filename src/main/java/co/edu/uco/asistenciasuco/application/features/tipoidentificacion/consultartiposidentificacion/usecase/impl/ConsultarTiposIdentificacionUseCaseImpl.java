package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.mapper.ConsultarTiposIdentificacionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Implementacion del caso de uso consultar tipos de identificacion.
 */
public final class ConsultarTiposIdentificacionUseCaseImpl implements ConsultarTiposIdentificacionUseCase {

    private final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort;

    public ConsultarTiposIdentificacionUseCaseImpl(
            final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort
    ) {
        if (ObjectHelper.isNull(tipoIdentificacionRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida TipoIdentificacionRepositoryPort es obligatorio.");
        }
        this.tipoIdentificacionRepositoryPort = tipoIdentificacionRepositoryPort;
    }

    @Override
    public List<TipoIdentificacionDomain> execute() {
        return ConsultarTiposIdentificacionRepositoryMapper.toDomains(
                tipoIdentificacionRepositoryPort.consultarTiposIdentificacion()
        );
    }
}
