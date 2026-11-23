package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionCatalogoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionCatalogoProjection;

import java.util.List;

public interface ConsultarCatalogosSistemaUseCase {

    List<CategoriaJustificacionCatalogoProjection> consultarCategoriasJustificacion();

    List<MotivoCancelacionCatalogoProjection> consultarMotivosCancelacion();
}
