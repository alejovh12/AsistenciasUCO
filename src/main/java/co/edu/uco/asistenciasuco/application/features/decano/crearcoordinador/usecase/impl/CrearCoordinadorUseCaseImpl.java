package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;
import java.util.UUID;

public final class CrearCoordinadorUseCaseImpl implements CrearCoordinadorUseCase {

    private final CoordinadorCommandPort commandPort;
    private final InstitutionalScopePort scopePort;

    public CrearCoordinadorUseCaseImpl(final CoordinadorCommandPort commandPort, final InstitutionalScopePort scopePort) {
        this.commandPort = Objects.requireNonNull(commandPort, "CoordinadorCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final CrearCoordinadorDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear coordinador es obligatorio.");
        }
        final UUID facultad = scopePort.findFacultadIdByDecanoUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));
        commandPort.crearCoordinador(UUID.randomUUID(), domain.getNumeroIdentificacion(), domain.getPrimerNombre(),
                domain.getSegundoNombre(), domain.getPrimerApellido(), domain.getSegundoApellido(), domain.getCorreo(),
                domain.getIdPrograma(), facultad, domain.getPassword());
    }
}
