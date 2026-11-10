package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.primaryports.ConsultarRevisionesDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.usecase.ConsultarRevisionesDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;

import java.util.Objects;

public final class ConsultarRevisionesDocenteUseCaseImpl implements ConsultarRevisionesDocenteUseCase {

    private final RevisionCancelacionRepositoryPort repositorio;

    public ConsultarRevisionesDocenteUseCaseImpl(final RevisionCancelacionRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RevisionCancelacionRepositoryPort es obligatorio.");
    }

    @Override
    public PaginaRevisiones execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        return repositorio.consultarDocente(comando.usuarioId(), comando.pagina() == null ? 1 : comando.pagina(),
                comando.tamano() == null ? 20 : comando.tamano(), comando.soloPendientes(), comando.estado(), comando.grupoId());
    }
}
