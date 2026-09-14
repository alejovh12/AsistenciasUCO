package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.ConsultarParametrosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.domain.ParametroDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.mapper.ConsultarParametrosRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarParametrosUseCaseImpl implements ConsultarParametrosUseCase {
    private final ParametroQueryPort port;
    public ConsultarParametrosUseCaseImpl(final ParametroQueryPort port) { this.port = Objects.requireNonNull(port); }
    @Override public List<ParametroDomain> execute() {
        return port.consultarParametros().stream()
                .map(ConsultarParametrosRepositoryMapper::toDomain)
                .toList();
    }
}
