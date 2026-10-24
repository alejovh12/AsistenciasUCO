package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.DecanoIndicadoresRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAforoGruposProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.ConsultarAforoGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGruposPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain;
import java.util.List;
import java.util.Objects;

public final class ConsultarAforoGruposUseCaseImpl implements ConsultarAforoGruposUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;

    private final InstitutionalScopePort scopePort;
    private final DecanoIndicadoresQueryPort queryPort;

    public ConsultarAforoGruposUseCaseImpl(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DecanoIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public AforoGruposPaginaDomain execute(final ConsultarAforoGruposDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los filtros de consulta son obligatorios.");
        }
        final int pagina = domain.pagina() == null ? PAGINA_POR_DEFECTO : domain.pagina();
        if (pagina < 1) {
            throw new ValidationException("El numero de pagina debe ser mayor o igual a 1.");
        }
        final int tamano = domain.tamano() == null ? TAMANO_POR_DEFECTO : domain.tamano();
        if (tamano < 1) {
            throw new ValidationException("El tamano de pagina debe ser mayor o igual a 1.");
        }
        final boolean soloHabilitados = domain.soloHabilitados() == null || domain.soloHabilitados();
        DecanoIndicadoresRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        final PaginaAforoGruposProjection resultado = queryPort.consultarAforo(domain.actorUsuarioId(), domain.idPeriodoAcademico(),
                domain.idPrograma(), soloHabilitados, pagina, tamano);
        final List<AforoGrupoDomain> grupos = resultado.grupos().stream()
                .map(p -> new AforoGrupoDomain(p.idGrupo(), p.codigoGrupo(), p.idAsignatura(), p.nombreAsignatura(), p.idPrograma(), p.nombrePrograma(), p.cupoMaximo(), p.matriculadosActivos(), p.cuposDisponibles(), p.tasaAforo(), p.grupoEstaHabilitado()))
                .toList();
        return new AforoGruposPaginaDomain(grupos, resultado.totalRegistros());
    }
}
