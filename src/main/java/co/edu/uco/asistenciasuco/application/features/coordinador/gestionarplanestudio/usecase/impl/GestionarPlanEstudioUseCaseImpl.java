package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.GestionarPlanEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

public final class GestionarPlanEstudioUseCaseImpl implements GestionarPlanEstudioUseCase {

    private final PlanEstudioCommandPort commandPort;
    private final PlanEstudioQueryPort queryPort;
    private final InstitutionalScopePort scopePort;

    public GestionarPlanEstudioUseCaseImpl(
            final PlanEstudioCommandPort commandPort,
            final PlanEstudioQueryPort queryPort,
            final InstitutionalScopePort scopePort
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "PlanEstudioCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "PlanEstudioQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void guardar(final PlanEstudioDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar plan de estudio es obligatorio.");
        }
        final UUID programa = resolverProgramaCoordinador(domain.usuario());
        commandPort.registrarOActualizarPlanEstudio(
                domain.idPlanEstudio() == null ? UUID.randomUUID() : domain.idPlanEstudio(),
                programa,
                domain.inp(),
                domain.usuario()
        );
    }

    /**
     * HU081: alterna el estado del plan. El estado actual se lee de los planes del programa del coordinador; un plan
     * de otro programa no se encuentra (no se revela su existencia). La DB revalida RBAC y titularidad.
     */
    @Override
    public void cambiarEstado(final UUID idPlanEstudio, final UUID usuario) {
        final UUID programa = resolverProgramaCoordinador(usuario);
        final PlanEstudioProjection plan = queryPort.consultarPlanesPorPrograma(programa).stream()
                .filter(candidato -> candidato.id().equals(idPlanEstudio))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El plan de estudio indicado no existe en el programa del coordinador."));
        commandPort.cambiarEstadoPlanEstudio(idPlanEstudio, !plan.estaActivoPlanEstudio(), usuario);
    }

    @Override
    public void agregarSemestre(final UUID idPlanEstudio, final UUID usuario) {
        resolverProgramaCoordinador(usuario);
        commandPort.agregarSemestrePlanEstudio(idPlanEstudio, usuario);
    }

    @Override
    public void quitarSemestre(final UUID idPlanEstudio, final Integer numeroSemestre, final UUID usuario) {
        if (ObjectHelper.isNull(numeroSemestre) || numeroSemestre <= 0) {
            throw new ValidationException("El numero de semestre debe ser un entero positivo.");
        }
        resolverProgramaCoordinador(usuario);
        commandPort.quitarSemestrePlanEstudio(idPlanEstudio, numeroSemestre, usuario);
    }

    private UUID resolverProgramaCoordinador(final UUID usuario) {
        return scopePort.findProgramaIdByCoordinadorUsuario(usuario)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
    }
}
