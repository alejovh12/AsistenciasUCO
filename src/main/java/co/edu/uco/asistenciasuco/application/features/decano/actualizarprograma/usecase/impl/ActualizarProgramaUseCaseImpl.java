package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.ActualizarProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain.ActualizarProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ProgramaAcademicoRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;
import java.util.UUID;

public final class ActualizarProgramaUseCaseImpl implements ActualizarProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final ProgramaAcademicoCommandPort commandPort;
    private final ProgramaAcademicoQueryPort queryPort;

    public ActualizarProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final ProgramaAcademicoCommandPort commandPort,
            final ProgramaAcademicoQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "ProgramaAcademicoCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ProgramaAcademicoQueryPort es obligatorio.");
    }

    @Override
    public ProgramaAcademicoDomain execute(final ActualizarProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del programa son obligatorios.");
        }
        ProgramaAcademicoRules.requerido(domain.id(), "El identificador del programa es obligatorio.");
        final String nombre = ProgramaAcademicoRules.nombreNormalizado(domain.nombre());
        ProgramaAcademicoRules.requerido(domain.idTipoPrograma(), "El tipo de programa es obligatorio.");
        ProgramaAcademicoRules.requerido(domain.idCoordinador(), "El coordinador del programa es obligatorio.");
        final UUID facultad = ProgramaAcademicoRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        commandPort.actualizar(new ProgramaAcademicoCommandPort.ActualizarProgramaCommand(domain.id(), nombre,
                domain.idTipoPrograma(), domain.idCoordinador(), domain.idModalidad(), domain.idJornada(), domain.actorUsuarioId()));
        return queryPort.consultarPorId(domain.id(), facultad)
                .map(ProgramaAcademicoMapper::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException("El programa academico especificado no existe en el sistema."));
    }
}
