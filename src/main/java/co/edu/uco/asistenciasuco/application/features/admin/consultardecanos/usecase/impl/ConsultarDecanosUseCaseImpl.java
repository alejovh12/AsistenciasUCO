package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.ConsultarDecanosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain.DecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.mapper.ConsultarDecanosRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarDecanosUseCaseImpl implements ConsultarDecanosUseCase {
    private final DecanoQueryPort decanoQueryPort;
    public ConsultarDecanosUseCaseImpl(final DecanoQueryPort decanoQueryPort) {
        this.decanoQueryPort = Objects.requireNonNull(decanoQueryPort, "DecanoQueryPort es obligatorio.");
    }
    @Override
    public List<DecanoDomain> execute() {
        return decanoQueryPort.consultarDecanos().stream()
                .map(ConsultarDecanosRepositoryMapper::toDomain)
                .toList();
    }
}
