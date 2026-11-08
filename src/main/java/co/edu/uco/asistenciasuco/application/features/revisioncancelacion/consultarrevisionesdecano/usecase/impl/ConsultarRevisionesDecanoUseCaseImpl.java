package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.usecase.ConsultarRevisionesDecanoUseCase;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;

import java.util.Objects;

public final class ConsultarRevisionesDecanoUseCaseImpl implements ConsultarRevisionesDecanoUseCase {

    private final RevisionCancelacionRepositoryPort repositorio;

    public ConsultarRevisionesDecanoUseCaseImpl(final RevisionCancelacionRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
    }

    @Override
    public PaginaRevisiones execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        return repositorio.consultarDecano(comando.usuarioId(), comando.pagina() == null ? 1 : comando.pagina(),
                comando.tamano() == null ? 20 : comando.tamano(), comando.soloPendientes(), comando.estado(), comando.programaId());
    }
}
