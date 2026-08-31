package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.ConsultarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper.ConsultarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar sesion.
 */
public final class ConsultarSesionUseCaseImpl implements ConsultarSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;

    public ConsultarSesionUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
    }

    @Override
    public SesionConsultadaEntity execute(final ConsultarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        return ConsultarSesionRepositoryMapper.toUseCaseEntity(
                sesionRepositoryPort.consultarSesion(
                        ConsultarSesionRepositoryMapper.toRepositoryDTO(domain)
                )
        );
    }
}