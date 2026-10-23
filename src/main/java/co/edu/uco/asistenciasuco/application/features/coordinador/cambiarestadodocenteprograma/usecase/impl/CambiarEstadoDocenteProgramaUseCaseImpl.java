package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.CambiarEstadoDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain.CambiarEstadoDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.CambiarEstadoDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * HU093 (D12): activa o inactiva el VINCULO del docente con el programa del coordinador. No depende de ningun puerto
 * de identidad: la cuenta de acceso no se deshabilita (la persona puede tener otros roles o programas; ver IAM-001).
 */
public final class CambiarEstadoDocenteProgramaUseCaseImpl implements CambiarEstadoDocenteProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaCommandPort commandPort;
    private final DocenteProgramaQueryPort queryPort;

    public CambiarEstadoDocenteProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "DocenteProgramaCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
    }

    @Override
    public DocenteProgramaDomain execute(final CambiarEstadoDocenteProgramaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para cambiar el estado del docente es obligatorio.");
        }
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));

        commandPort.cambiarEstado(new CambiarEstadoDocenteProgramaCommand(
                domain.idDocente(), idPrograma, domain.estaActivo(), domain.actorUsuarioId()));

        return queryPort.consultar(domain.actorUsuarioId(),
                        new DocentesProgramaFiltro(idPrograma, 1, 1, null, null, null, domain.idDocente(), null))
                .elementos().stream().findFirst()
                .map(DocenteProgramaMapper::toDomain)
                .orElseThrow(() -> new InternalApplicationException("No fue posible leer el vinculo del docente despues de cambiar su estado."));
    }
}
