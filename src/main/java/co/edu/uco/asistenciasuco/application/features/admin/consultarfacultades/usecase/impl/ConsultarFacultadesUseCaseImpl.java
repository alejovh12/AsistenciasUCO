package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.ConsultarFacultadesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.domain.FacultadDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.mapper.ConsultarFacultadesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarFacultadesUseCaseImpl implements ConsultarFacultadesUseCase {
    private final FacultadQueryPort port;
    public ConsultarFacultadesUseCaseImpl(final FacultadQueryPort port) { this.port = Objects.requireNonNull(port); }
    @Override public List<FacultadDomain> execute() {
        return port.consultarFacultades().stream()
                .map(ConsultarFacultadesRepositoryMapper::toDomain)
                .toList();
    }
}
