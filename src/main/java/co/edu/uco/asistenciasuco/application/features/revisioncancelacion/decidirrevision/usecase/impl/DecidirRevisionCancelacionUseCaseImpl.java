package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.usecase.DecidirRevisionCancelacionUseCase;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;

import java.util.Objects;

public final class DecidirRevisionCancelacionUseCaseImpl implements DecidirRevisionCancelacionUseCase {

    private final RevisionCancelacionRepositoryPort repositorio;
    private final AvisoRevisionCancelacionService avisos;

    public DecidirRevisionCancelacionUseCaseImpl(final RevisionCancelacionRepositoryPort repositorio, final AvisoRevisionCancelacionService avisos) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
        this.avisos = Objects.requireNonNull(avisos, "AvisoRevisionCancelacionService es obligatorio.");
    }

    @Override
    public ResultadoDecision execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        final ResultadoDecision resultado = repositorio.decidir(comando.revisionId(), comando.decision(), comando.motivo(), comando.usuarioEjecutor());
        avisos.avisarDecision(resultado, comando.usuarioEjecutor());
        return resultado;
    }
}
