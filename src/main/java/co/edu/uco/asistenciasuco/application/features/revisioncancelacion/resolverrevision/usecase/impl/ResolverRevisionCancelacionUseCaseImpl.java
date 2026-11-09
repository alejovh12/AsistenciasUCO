package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.usecase.ResolverRevisionCancelacionUseCase;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;

import java.util.Objects;

public final class ResolverRevisionCancelacionUseCaseImpl implements ResolverRevisionCancelacionUseCase {

    private final RevisionCancelacionRepositoryPort repositorio;
    private final AvisoRevisionCancelacionService avisos;

    public ResolverRevisionCancelacionUseCaseImpl(final RevisionCancelacionRepositoryPort repositorio, final AvisoRevisionCancelacionService avisos) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
        this.avisos = Objects.requireNonNull(avisos, "AvisoRevisionCancelacionService es obligatorio.");
    }

    @Override
    public ResultadoResolucion execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        final ResultadoResolucion resultado = repositorio.resolver(comando.revisionId(), comando.decision(), comando.motivo(), comando.usuarioEjecutor());
        avisos.avisarResolucion(resultado, comando.usuarioEjecutor());
        return resultado;
    }
}
