package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.domain.CategoriaJustificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.UUID;

public interface GestionarCategoriaJustificacionUseCase {

    List<CategoriaJustificacionAdminProjection> consultar();

    ResultadoCatalogoAdminProjection crear(CategoriaJustificacionDomain domain);

    ResultadoCatalogoAdminProjection actualizar(CategoriaJustificacionDomain domain);

    ResultadoCatalogoAdminProjection cambiarEstado(UUID id, Boolean estaActiva, UUID usuarioEjecutor);
}
