package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper.CrearSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso crear sesion.
 */
public final class CrearSesionUseCaseImpl implements CrearSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;

    public CrearSesionUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
    }

    @Override
    public void execute(final CrearSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }
        sesionRepositoryPort.crearSesion(CrearSesionRepositoryMapper.toRepositoryDTO(domain));
    }
}