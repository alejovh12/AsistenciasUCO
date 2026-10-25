package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocentesProgramaPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.ConsultarDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarDocentesProgramaUseCaseImpl implements ConsultarDocentesProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaQueryPort queryPort;

    public ConsultarDocentesProgramaUseCaseImpl(final InstitutionalScopePort scopePort, final DocenteProgramaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
    }

    @Override
    public DocentesProgramaPaginaDomain execute(final UUID actorUsuarioId, final ConsultarDocentesProgramaDomain consulta) {
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        final DocentesProgramaPagina pagina = queryPort.consultar(actorUsuarioId, new DocentesProgramaFiltro(idPrograma,
                consulta.pagina(), consulta.tamano(), consulta.busqueda(), consulta.estaActivo(),
                consulta.idTipoVinculacion(), null, null));
        return new DocentesProgramaPaginaDomain(
                pagina.elementos().stream().map(DocenteProgramaMapper::toDomain).toList(), pagina.total());
    }
}
