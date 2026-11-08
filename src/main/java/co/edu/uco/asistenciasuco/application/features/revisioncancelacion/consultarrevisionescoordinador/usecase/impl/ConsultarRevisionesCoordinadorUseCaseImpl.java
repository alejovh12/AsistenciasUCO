package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.usecase.ConsultarRevisionesCoordinadorUseCase;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;

import java.util.Objects;

public final class ConsultarRevisionesCoordinadorUseCaseImpl implements ConsultarRevisionesCoordinadorUseCase {

    private final RevisionCancelacionRepositoryPort repositorio;

    public ConsultarRevisionesCoordinadorUseCaseImpl(final RevisionCancelacionRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
    }

    @Override
    public PaginaRevisiones execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        return repositorio.consultarCoordinador(comando.usuarioId(), comando.pagina() == null ? 1 : comando.pagina(),
                comando.tamano() == null ? 20 : comando.tamano(), comando.soloPendientes(), comando.estado(), comando.programaId(), comando.grupoId());
    }
}
