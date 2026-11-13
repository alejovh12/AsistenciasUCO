package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.CambiarEstadoFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain.CambiarEstadoFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;

import java.util.Objects;

public class CambiarEstadoFacultadUseCaseImpl implements CambiarEstadoFacultadUseCase {

    private final FacultadCommandPort facultadCommandPort;

    public CambiarEstadoFacultadUseCaseImpl(final FacultadCommandPort facultadCommandPort) {
        this.facultadCommandPort = Objects.requireNonNull(facultadCommandPort, "El puerto de comando de facultades es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoFacultadDomain domain) {
        if (domain == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_001);
        }
        if (domain.id() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_002);
        }
        if (domain.activa() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_009);
        }

        facultadCommandPort.cambiarEstadoFacultad(new FacultadCommandPort.CambiarEstadoFacultadCommand(
                domain.id(),
                domain.activa(),
                domain.actorUsuarioId()
        ));
    }
}
