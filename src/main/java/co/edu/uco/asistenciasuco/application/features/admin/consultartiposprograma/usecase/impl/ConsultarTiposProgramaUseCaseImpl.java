package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain.TipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;

import java.util.List;
import java.util.Objects;

public class ConsultarTiposProgramaUseCaseImpl implements ConsultarTiposProgramaUseCase {

    private final TipoProgramaQueryPort queryPort;

    public ConsultarTiposProgramaUseCaseImpl(final TipoProgramaQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "El puerto de consulta de tipos de programa es obligatorio.");
    }

    @Override
    public List<TipoProgramaDomain> execute(final boolean soloActivos) {
        return queryPort.consultarTiposPrograma().stream()
                .filter(tipo -> !soloActivos || tipo.estaActivo())
                .map(ConsultarTiposProgramaUseCaseImpl::toDomain)
                .toList();
    }

    private static TipoProgramaDomain toDomain(final TipoProgramaProjection projection) {
        return new TipoProgramaDomain(projection.id(), projection.codigo(), projection.nombre(), projection.estaActivo());
    }
}
