package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.ConsultarTiposVinculacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.domain.TipoVinculacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoVinculacionQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarTiposVinculacionUseCaseImpl implements ConsultarTiposVinculacionUseCase {

    private final TipoVinculacionQueryPort queryPort;

    public ConsultarTiposVinculacionUseCaseImpl(final TipoVinculacionQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "TipoVinculacionQueryPort es obligatorio.");
    }

    @Override
    public List<TipoVinculacionDomain> execute() {
        return queryPort.consultarActivos().stream().map(p -> new TipoVinculacionDomain(p.id(), p.codigo(), p.nombre())).toList();
    }
}
