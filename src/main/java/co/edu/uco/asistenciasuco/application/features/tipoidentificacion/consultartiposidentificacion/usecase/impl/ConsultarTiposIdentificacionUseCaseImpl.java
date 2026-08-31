package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.mapper.ConsultarTiposIdentificacionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar tipos de identificacion.
 */
public final class ConsultarTiposIdentificacionUseCaseImpl implements ConsultarTiposIdentificacionUseCase {

    private final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort;

    public ConsultarTiposIdentificacionUseCaseImpl(
            final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort
    ) {
        this.tipoIdentificacionRepositoryPort = Objects.requireNonNull(tipoIdentificacionRepositoryPort, "El puerto de salida TipoIdentificacionRepositoryPort es obligatorio.");
    }

    @Override
    public List<TipoIdentificacionDomain> execute() {
        return ConsultarTiposIdentificacionRepositoryMapper.toDomains(
                tipoIdentificacionRepositoryPort.consultarTiposIdentificacion()
        );
    }
}