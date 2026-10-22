package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.ConsultarCatalogosProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.domain.CatalogoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CatalogoProgramaProjection;
import java.util.List;
import java.util.Objects;

public final class ConsultarCatalogosProgramaUseCaseImpl implements ConsultarCatalogosProgramaUseCase {

    private final CatalogoProgramaQueryPort queryPort;

    public ConsultarCatalogosProgramaUseCaseImpl(final CatalogoProgramaQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "CatalogoProgramaQueryPort es obligatorio.");
    }

    @Override
    public List<CatalogoProgramaDomain> tiposPrograma() {
        return toDomain(queryPort.consultarTiposPrograma());
    }

    @Override
    public List<CatalogoProgramaDomain> modalidades() {
        return toDomain(queryPort.consultarModalidades());
    }

    @Override
    public List<CatalogoProgramaDomain> jornadas() {
        return toDomain(queryPort.consultarJornadas());
    }

    private static List<CatalogoProgramaDomain> toDomain(final List<CatalogoProgramaProjection> items) {
        return items.stream().map(i -> new CatalogoProgramaDomain(i.id(), i.codigo(), i.nombre())).toList();
    }
}
