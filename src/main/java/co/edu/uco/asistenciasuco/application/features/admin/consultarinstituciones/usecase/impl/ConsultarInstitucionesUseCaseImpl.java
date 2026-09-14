package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.ConsultarInstitucionesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.domain.InstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.mapper.ConsultarInstitucionesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarInstitucionesUseCaseImpl implements ConsultarInstitucionesUseCase {
    private final InstitucionQueryPort port;
    public ConsultarInstitucionesUseCaseImpl(final InstitucionQueryPort port) { this.port = Objects.requireNonNull(port); }
    @Override public List<InstitucionDomain> execute() {
        return port.consultarInstituciones().stream()
                .map(ConsultarInstitucionesRepositoryMapper::toDomain)
                .toList();
    }
}
