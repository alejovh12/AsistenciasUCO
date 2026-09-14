package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.ConsultarAreasUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.domain.AreaDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.mapper.ConsultarAreasRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarAreasUseCaseImpl implements ConsultarAreasUseCase {
    private final AreaQueryPort port;
    public ConsultarAreasUseCaseImpl(final AreaQueryPort port) { this.port = Objects.requireNonNull(port); }
    @Override public List<AreaDomain> execute() {
        return port.consultarAreas().stream().map(ConsultarAreasRepositoryMapper::toDomain).toList();
    }
}
