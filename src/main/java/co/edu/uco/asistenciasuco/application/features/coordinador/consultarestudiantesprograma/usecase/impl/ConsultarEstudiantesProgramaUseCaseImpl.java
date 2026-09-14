package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.ConsultarEstudiantesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.mapper.ConsultarEstudiantesProgramaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarEstudiantesProgramaUseCaseImpl implements ConsultarEstudiantesProgramaUseCase {
    private final InstitutionalScopePort scopePort; private final EstudianteProgramaQueryPort queryPort;
    public ConsultarEstudiantesProgramaUseCaseImpl(final InstitutionalScopePort scopePort, final EstudianteProgramaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<EstudianteProgramaDomain> execute(final UUID actorUsuarioId) {
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        return queryPort.consultarEstudiantesPorPrograma(idPrograma).stream().map(ConsultarEstudiantesProgramaRepositoryMapper::toDomain).toList();
    }
}
