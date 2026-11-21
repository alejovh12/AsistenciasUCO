package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.ConsultarCatalogosSistemaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogosSistemaConsultaPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionCatalogoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionCatalogoProjection;

import java.util.List;
import java.util.Objects;

/** Lectura de los catalogos del sistema para el ADMINISTRADOR; no hay escritura: los define el sistema. */
public final class ConsultarCatalogosSistemaUseCaseImpl implements ConsultarCatalogosSistemaUseCase {

    private final CatalogosSistemaConsultaPort port;

    public ConsultarCatalogosSistemaUseCaseImpl(final CatalogosSistemaConsultaPort port) {
        this.port = Objects.requireNonNull(port, "CatalogosSistemaConsultaPort es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionCatalogoProjection> consultarCategoriasJustificacion() {
        return port.consultarCategoriasJustificacion();
    }

    @Override
    public List<MotivoCancelacionCatalogoProjection> consultarMotivosCancelacion() {
        return port.consultarMotivosCancelacion();
    }
}
