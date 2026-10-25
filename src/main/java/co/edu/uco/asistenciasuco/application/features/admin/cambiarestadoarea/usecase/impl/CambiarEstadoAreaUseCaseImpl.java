package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.CambiarEstadoAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.domain.CambiarEstadoAreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;

import java.util.Objects;

public class CambiarEstadoAreaUseCaseImpl implements CambiarEstadoAreaUseCase {

    private final AreaCommandPort areaCommandPort;

    public CambiarEstadoAreaUseCaseImpl(final AreaCommandPort areaCommandPort) {
        this.areaCommandPort = Objects.requireNonNull(areaCommandPort, "El puerto de comando de areas es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoAreaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del area de conocimiento son obligatorios.");
        }
        if (domain.id() == null) {
            throw new ValidationException("El identificador del area de conocimiento es obligatorio.");
        }
        if (domain.activa() == null) {
            throw new ValidationException("El estado deseado del area de conocimiento es obligatorio.");
        }
        areaCommandPort.cambiarEstadoArea(new AreaCommandPort.CambiarEstadoAreaCommand(domain.id(), domain.activa(), domain.actorUsuarioId()));
    }
}
