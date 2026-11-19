package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.GestionarAsignaturaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.EstadoAsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.MensajeEstadoAsignatura;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoEstadoAsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GestionarAsignaturaUseCaseImpl implements GestionarAsignaturaUseCase {

    private final AsignaturaCommandPort commandPort;
    private final AsignaturaQueryPort queryPort;
    private final PlanEstudioQueryPort planQueryPort;
    private final InstitutionalScopePort scopePort;
    private final MessageCatalogPort mensajes;

    public GestionarAsignaturaUseCaseImpl(
            final AsignaturaCommandPort commandPort,
            final AsignaturaQueryPort queryPort,
            final PlanEstudioQueryPort planQueryPort,
            final InstitutionalScopePort scopePort,
            final MessageCatalogPort mensajes
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "AsignaturaCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "AsignaturaQueryPort es obligatorio.");
        this.planQueryPort = Objects.requireNonNull(planQueryPort, "PlanEstudioQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public void crear(final AsignaturaDomain domain) {
        validar(domain);
        final UUID programa = resolverPrograma(domain.usuarioEjecutor());
        validarPlanDelPrograma(domain.idPlanEstudio(), programa);
        commandPort.crearAsignatura(UUID.randomUUID(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente(), domain.codigoCaracter(), domain.usuarioEjecutor());
    }

    @Override
    public void actualizar(final AsignaturaDomain domain) {
        validar(domain);
        final UUID programa = resolverPrograma(domain.usuarioEjecutor());
        validarPlanDelPrograma(domain.idPlanEstudio(), programa);
        final boolean perteneceAlPlan = queryPort.consultarAsignaturasPorPlan(domain.idPlanEstudio()).stream()
                .anyMatch(asignatura -> domain.idAsignatura().equals(asignatura.id())
                        && programa.equals(asignatura.idPrograma()));
        if (!perteneceAlPlan) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_009);
        }
        commandPort.actualizarAsignatura(domain.idAsignatura(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente(), domain.codigoCaracter(), domain.usuarioEjecutor());
    }

    @Override
    public EstadoAsignaturaDomain toggleEstado(final UUID asignaturaId, final Boolean estaActivaDeseada, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(asignaturaId)) {
            throw new CrosscuttingException("La asignatura es obligatoria.");
        }
        final UUID programa = resolverPrograma(usuarioEjecutor);
        final AsignaturaProjection actual = queryPort.consultarAsignaturasPorPrograma(programa).stream()
                .filter(asignatura -> asignaturaId.equals(asignatura.id())
                        && programa.equals(asignatura.idPrograma()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_009));
        // HU074: si el SP rechaza (ASI_009, ASI_006...) la excepcion sale antes de aqui. El estado en que quedo lo dice el codigo
        // de exito del SP (ASI_011 / ASI_012); sin codigo (SP anterior), el estado pedido y, si se pidio alternar, el contrario
        // al leido en la validacion de ambito.
        final ResultadoEstadoAsignaturaProjection resultado =
                commandPort.toggleEstadoAsignatura(asignaturaId, estaActivaDeseada, usuarioEjecutor);
        final boolean quedaActiva = Optional.ofNullable(resultado.estaActivaResultante())
                .orElseGet(() -> Objects.requireNonNullElse(estaActivaDeseada, !actual.estaActivaAsignatura()));
        return new EstadoAsignaturaDomain(asignaturaId, quedaActiva, mensajeConfirmacion(resultado.mensajeUsuario(), quedaActiva));
    }

    /** Mensaje del SP; si llega vacio, el del catalogo (ASI_011/ASI_012) y, sin catalogo, el respaldo del codigo. */
    private String mensajeConfirmacion(final String mensajeDb, final boolean quedaActiva) {
        if (!TextHelper.isNullOrBlank(mensajeDb)) {
            return mensajeDb.strip();
        }
        final MensajeEstadoAsignatura codigo = MensajeEstadoAsignatura.para(quedaActiva);
        return mensajes.findUserMessage(codigo.codigo())
                .filter(texto -> !TextHelper.isNullOrBlank(texto))
                .orElse(codigo.respaldo());
    }

    private UUID resolverPrograma(final UUID usuarioEjecutor) {
        return Objects.requireNonNullElseGet(
                scopePort.findProgramaIdByCoordinadorUsuario(usuarioEjecutor).orElse(null),
                () -> { throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_006); }
        );
    }

    private void validarPlanDelPrograma(final UUID planId, final UUID programa) {
        if (ObjectHelper.isNull(planId)
                || planQueryPort.consultarPlanesPorPrograma(programa).stream()
                .noneMatch(plan -> planId.equals(plan.id()) && programa.equals(plan.idPrograma()))) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_010);
        }
    }

    private static void validar(final AsignaturaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar asignatura es obligatorio.");
        }
    }
}
