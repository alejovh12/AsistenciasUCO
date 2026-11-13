package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.CrearFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.domain.CrearFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import java.util.Objects;
import java.util.UUID;

public class CrearFacultadUseCaseImpl implements CrearFacultadUseCase {

    private final FacultadCommandPort facultadCommandPort;

    public CrearFacultadUseCaseImpl(final FacultadCommandPort facultadCommandPort) {
        this.facultadCommandPort = Objects.requireNonNull(facultadCommandPort, "El puerto de comando de facultades es obligatorio.");
    }

    @Override
    public void execute(final CrearFacultadDomain domain) {
        if (domain == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_001);
        }

        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_014);
        }

        if (domain.idDecano() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_015);
        }

        final UUID idFacultad = domain.id() != null ? domain.id() : UUID.randomUUID();

        facultadCommandPort.crearFacultad(new FacultadCommandPort.CrearFacultadCommand(
                idFacultad,
                nombre,
                domain.idInstitucion(),
                domain.idDecano(),
                domain.actorUsuarioId()
        ));
    }
}
