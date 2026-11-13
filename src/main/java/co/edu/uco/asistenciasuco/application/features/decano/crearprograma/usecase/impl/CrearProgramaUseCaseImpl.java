package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ProgramaAcademicoRules;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;
import java.util.UUID;

public final class CrearProgramaUseCaseImpl implements CrearProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final ProgramaAcademicoCommandPort commandPort;
    private final ProgramaAcademicoQueryPort queryPort;

    public CrearProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final ProgramaAcademicoCommandPort commandPort,
            final ProgramaAcademicoQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "ProgramaAcademicoCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ProgramaAcademicoQueryPort es obligatorio.");
    }

    @Override
    public ProgramaAcademicoDomain execute(final CrearProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_043);
        }
        final String nombre = ProgramaAcademicoRules.nombreNormalizado(domain.nombre());
        ProgramaAcademicoRules.requerido(domain.idTipoPrograma(), MensajeUsuarioErrorCode.VAL_APP_064);
        ProgramaAcademicoRules.requerido(domain.idCoordinador(), MensajeUsuarioErrorCode.VAL_APP_065);
        final UUID facultad = ProgramaAcademicoRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        final UUID id = UUID.randomUUID();
        commandPort.crear(new ProgramaAcademicoCommandPort.CrearProgramaCommand(id, facultad, nombre, domain.idTipoPrograma(),
                domain.idCoordinador(), domain.idModalidad(), domain.idJornada(), domain.actorUsuarioId()));
        return queryPort.consultarPorId(id, facultad)
                .map(ProgramaAcademicoMapper::toDomain)
                .orElseThrow(() -> new InternalApplicationException("No fue posible leer el programa academico recien creado."));
    }
}
