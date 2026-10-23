package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.CambiarEstadoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain.CambiarEstadoProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ProgramaAcademicoRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;
import java.util.UUID;

public final class CambiarEstadoProgramaUseCaseImpl implements CambiarEstadoProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final ProgramaAcademicoCommandPort commandPort;
    private final ProgramaAcademicoQueryPort queryPort;

    public CambiarEstadoProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final ProgramaAcademicoCommandPort commandPort,
            final ProgramaAcademicoQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "ProgramaAcademicoCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ProgramaAcademicoQueryPort es obligatorio.");
    }

    @Override
    public ProgramaAcademicoDomain execute(final CambiarEstadoProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del programa son obligatorios.");
        }
        ProgramaAcademicoRules.requerido(domain.id(), "El identificador del programa es obligatorio.");
        ProgramaAcademicoRules.requerido(domain.estaActivo(), "El estado deseado del programa es obligatorio.");
        final UUID facultad = ProgramaAcademicoRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        commandPort.cambiarEstado(new ProgramaAcademicoCommandPort.CambiarEstadoProgramaCommand(domain.id(), domain.estaActivo(), domain.actorUsuarioId()));
        return queryPort.consultarPorId(domain.id(), facultad)
                .map(ProgramaAcademicoMapper::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException("El programa academico especificado no existe en el sistema."));
    }
}
